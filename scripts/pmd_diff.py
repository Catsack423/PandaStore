"""Collect reproducible PR evidence and summarize PMD without third-party packages."""

import argparse
from collections import Counter
import csv
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

SCHEMA_VERSION = 2
CHECK_SCOPE = "violations_overlapping_changed_lines"
CHECK_SCOPE_DESCRIPTION = "Only unsuppressed violations overlapping changed lines affect the check; outside findings are informational only."
JAVA_ROOT = "backend/src/main/java/"
RULESET = "config/pmd/ruleset.xml"


def now():
    return datetime.now(timezone.utc).isoformat()


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def git(root, *args):
    return subprocess.check_output(
        ["git", "--literal-pathspecs", "-C", str(root), *args], stderr=subprocess.PIPE
    )


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def java_path(path):
    return path.startswith(JAVA_ROOT) and path.endswith(".java")


def pr_context(event):
    pr = event.get("pull_request", {})
    return {
        "number": pr.get("number", event.get("number")),
        "url": pr.get("html_url"), "title": pr.get("title"),
        "author": pr.get("user", {}).get("login"), "draft": pr.get("draft"),
        "created_at": pr.get("created_at"), "updated_at": pr.get("updated_at"),
        "head_branch": pr.get("head", {}).get("ref"),
        "head_repository": (pr.get("head", {}).get("repo") or {}).get("full_name"),
        "base_branch": pr.get("base", {}).get("ref"),
        "base_repository": (pr.get("base", {}).get("repo") or {}).get("full_name"),
        "labels": [label["name"] for label in pr.get("labels", [])],
    }


def prepare(root, output, base, head, event=None):
    output.mkdir(parents=True, exist_ok=True)
    started = now()
    write_json(output / "status.json", {"schema_version": SCHEMA_VERSION, "status": "preparing", "started_at": started})
    # Resolve immutable object IDs; never interpolate a branch name into shell code.
    for ref in (base, head):
        if not re.fullmatch(r"[0-9a-fA-F]{40}", ref):
            raise ValueError("Base and head must be full commit SHA values")
    base = git(root, "rev-parse", "--verify", f"{base}^{{commit}}").decode().strip()
    head = git(root, "rev-parse", "--verify", f"{head}^{{commit}}").decode().strip()
    if git(root, "rev-parse", "HEAD").decode().strip() != head:
        raise ValueError("Checkout must match the requested PR head")
    merge_base = git(root, "merge-base", base, head).decode().strip()
    tokens = git(root, "diff", "--name-status", "-z", "--find-renames", merge_base, head).decode("utf-8").split("\0")
    changes, selected = [], []
    i = 0
    while i < len(tokens) - 1:
        status, path = tokens[i:i + 2]
        i += 2
        old_path = None
        if status.startswith(("R", "C")):
            old_path, path = path, tokens[i]
            i += 1
        entry = {"status": status, "path": path, "old_path": old_path, "scanned": False}
        if java_path(path) or (old_path and java_path(old_path)):
            paths = [p for p in (old_path, path) if p]
            stat = git(root, "diff", "--numstat", "--find-renames", merge_base, head, "--", *paths).decode("utf-8")
            counts = [line.split("\t", 2)[:2] for line in stat.splitlines()]
            entry.update({"added_lines": sum(int(a) for a, _ in counts if a != "-"),
                          "deleted_lines": sum(int(d) for _, d in counts if d != "-")})
        if java_path(path) and status[0] in "AMR":
            if any(char in path for char in "\n\r,"):
                raise ValueError("PMD file lists cannot safely represent newline or comma in a filename")
            source = root / path
            if source.is_symlink() or any(parent.is_symlink() for parent in source.parents):
                raise ValueError("Changed Java files must not be symlinks")
            source.resolve().relative_to(root.resolve())
            if not source.is_file():
                raise ValueError(f"Changed Java input is missing: {path}")
            content = source.read_bytes()
            blob = git(root, "hash-object", f"--path={path}", "--", path).decode().strip()
            if blob != git(root, "rev-parse", f"{head}:{path}").decode().strip():
                raise ValueError(f"Working file differs from the PR head: {path}")
            patch = git(root, "diff", "--no-ext-diff", "--no-textconv", "--unified=0", "--find-renames", merge_base, head, "--", *[p for p in (old_path, path) if p]).decode("utf-8")
            ranges = []
            for start, length in re.findall(r"^@@ .*?\+(\d+)(?:,(\d+))? @@", patch, re.MULTILINE):
                count = int(length) if length else 1
                if count:
                    ranges.append([int(start), int(start) + count - 1])
            entry.update({"scanned": True, "sha256": sha256(content), "git_blob_sha": blob, "changed_line_ranges": ranges})
            selected.append(path)
        changes.append(entry)
    java_changes = [c for c in changes if java_path(c["path"]) or (c["old_path"] and java_path(c["old_path"]))]
    patch_paths = sorted({p for c in java_changes for p in (c["path"], c["old_path"]) if p})
    patch = git(root, "diff", "--no-ext-diff", "--no-textconv", "--find-renames", merge_base, head, "--", *patch_paths) if patch_paths else b""
    (output / "java.diff.patch").write_bytes(patch)
    (output / "files.txt").write_text("".join(path + "\n" for path in selected), encoding="utf-8")
    rules = (root / RULESET).read_bytes()
    (output / "ruleset.xml").write_bytes(rules)
    write_json(output / "changed-files.json", changes)
    commit_tokens = git(root, "log", "--format=%H%x00%an%x00%aI%x00%cn%x00%cI%x00%s%x00", f"{merge_base}..{head}").decode("utf-8").split("\0")
    commits = []
    for i in range(0, len(commit_tokens) - 1, 6):
        values = commit_tokens[i:i + 6]
        values[0] = values[0].strip()
        commits.append(dict(zip(("sha", "author", "authored_at", "committer", "committed_at", "subject"), values)))
    write_json(output / "commits.json", commits)
    run_keys = ("GITHUB_REPOSITORY", "GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_ACTOR", "GITHUB_EVENT_NAME", "GITHUB_WORKFLOW", "GITHUB_JOB", "GITHUB_SHA", "RUNNER_OS", "RUNNER_ARCH")
    run = {key.lower(): os.environ.get(key) for key in run_keys}
    if run["github_repository"] and run["github_run_id"]:
        run["url"] = f"{os.environ.get('GITHUB_SERVER_URL', 'https://github.com')}/{run['github_repository']}/actions/runs/{run['github_run_id']}"
    metadata = {"schema_version": SCHEMA_VERSION, "started_at": started,
                "pull_request": pr_context(event or {}), "run": run,
                "comparison": {"base_sha": base, "head_sha": head, "merge_base_sha": merge_base, "mode": "three-dot"},
                "scope": {"path": JAVA_ROOT + "**/*.java", "statuses": ["A", "M", "R"], "file_count": len(selected), "files": selected},
                "tool": {"pmd_version": os.environ.get("PMD_VERSION", "7.10.0"), "distribution_sha256": os.environ.get("PMD_SHA256"), "java_language_version": "21", "minimum_priority": 5, "ruleset_sha256": sha256(rules), "aux_classpath": None},
                "retention_days": 90}
    write_json(output / "metadata.json", metadata)
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as handle:
            handle.write(f"file_count={len(selected)}\n")
    return metadata


def markdown(value):
    return str(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("|", "\\|").replace("`", "\\`").replace("\n", " ").replace("\r", " ")


def csv_cell(value):
    text = str(value) if value is not None else ""
    # Prevent source-controlled messages/paths from becoming spreadsheet formulas.
    return "'" + text if text.lstrip().startswith(("=", "+", "-", "@")) else text


def report_path(root, filename):
    filename = filename.replace("\\", "/")
    if Path(filename).is_absolute():
        filename = Path(filename).resolve().relative_to(root.resolve()).as_posix()
    return filename[2:] if filename.startswith("./") else filename


def finding_key(finding):
    return (finding["file"], finding.get("rule"), *[
        int(finding.get(key, 0)) for key in ("beginline", "endline", "begincolumn", "endcolumn")])


def author_identity(name, mail):
    """Identify Git authors without storing their email or equating names with logins."""
    mail = mail.strip("<>").strip().casefold()
    match = re.fullmatch(r"(?:\d+\+)?([a-z0-9-]+(?:\[bot\])?)@users\.noreply\.github\.com", mail)
    login = match.group(1) if match else None
    identity = "github:" + login if login else "git-email-sha256:" + sha256(mail.encode("utf-8")) if mail else "git-name:" + name
    return {"id": identity, "name": name, "github_login": login,
            "github_login_source": "github_noreply_email" if login else None}


class NoApiRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None  # Never forward the read-only token to a redirected host.


class GithubAuthors:
    """Optional account association for exact commit SHAs; never infer from PR opener."""

    def __init__(self, warnings):
        self.warnings = warnings
        self.token = os.environ.get("PMD_GITHUB_TOKEN", "")
        self.repository = os.environ.get("GITHUB_REPOSITORY", "")
        self.enabled = bool(self.token and os.environ.get("GITHUB_ACTIONS") == "true"
                            and os.environ.get("GITHUB_API_URL", "https://api.github.com") == "https://api.github.com"
                            and re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", self.repository))
        self.calls = 0

    def enrich(self, sha, mail, author):
        if not self.enabled:
            return author
        if self.calls >= 100:
            self.enabled = False
            self.warnings.append("github_author_lookup_limit: remaining authors use noreply or null")
            return author
        self.calls += 1
        request = urllib.request.Request(f"https://api.github.com/repos/{self.repository}/commits/{sha}", headers={
            "Authorization": "Bearer " + self.token, "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "PandaStore-PMD-attribution"})
        try:
            with urllib.request.build_opener(NoApiRedirect()).open(request, timeout=5) as response:
                data = json.load(response)
            # Bind the associated account to both the requested object and its Git author.
            api_mail = data.get("commit", {}).get("author", {}).get("email")
            login = (data.get("author") or {}).get("login")
            if (data.get("sha") == sha and isinstance(api_mail, str) and mail.strip().casefold() == api_mail.strip().casefold()
                    and isinstance(login, str) and re.fullmatch(r"[A-Za-z0-9-]+(?:\[bot\])?", login)):
                return {**author, "github_login": login, "github_login_source": "github_commit_api"}
        except (OSError, ValueError, TypeError, AttributeError, urllib.error.URLError):
            self.enabled = False  # Avoid repeated timeouts/rate-limit failures; no raw response or email in evidence.
            self.warnings.append("github_author_lookup_unavailable: authors use noreply or null; check status unchanged")
        return author


class AttributionHistory:
    """Infer PR-spine vs merged-side provenance, not verified human ownership."""

    def __init__(self, root, comparison):
        self.root = root
        self.head = comparison.get("head_sha")
        self.base = comparison.get("base_sha")
        self.files, self.commits = {}, {}
        self.warnings = []
        self.github_authors = GithubAuthors(self.warnings)
        self.ready = False
        try:
            if git(root, "rev-parse", "--is-shallow-repository").decode().strip() != "false":
                self.warnings.append("shallow_history: fetch-depth: 0 is required; findings become unknown + low")
                return
            self.eligible = set(git(root, "rev-list", self.head, "--not", self.base).decode().splitlines())
            self.own = set(git(root, "rev-list", "--first-parent", self.head, "--not", self.base).decode().splitlines())
            self.ready = True
        except (OSError, subprocess.SubprocessError, ValueError, TypeError) as error:
            self.warnings.append("incomplete_git_history: " + type(error).__name__ + "; base/head/parents unavailable; findings become unknown + low")

    def blame(self, filename):
        if filename not in self.files:
            lines = {}
            try:
                data = git(self.root, "-c", "blame.blankBoundary=false", "blame", "--line-porcelain",
                           "--encoding=UTF-8", "--ignore-revs-file", "", self.head, "--", filename).decode("utf-8")
                current = None
                for line in data.splitlines():
                    header = re.fullmatch(r"([0-9a-f]{40}) (\d+) (\d+)(?: (\d+))?", line)
                    if header:
                        current = {"commit": header.group(1), "line": int(header.group(3))}
                    elif current is not None and line.startswith("author "):
                        current["name"] = line[7:]
                    elif current is not None and line.startswith("author-mail "):
                        current["mail"] = line[12:]
                    elif current is not None and line.startswith("\t"):
                        lines[current["line"]] = current
                        current = None
            except (OSError, subprocess.SubprocessError, UnicodeError, ValueError, TypeError):
                self.warnings.append(f"blame_unavailable: {filename}")
            self.files[filename] = lines
        return self.files[filename]

    def classify(self, blamed):
        sha = blamed["commit"]
        if sha not in self.commits:
            record = {"commit": sha, "category": "unknown", "confidence": "low", "reason": "commit_unavailable", "author": None}
            try:
                fields = git(self.root, "show", "--no-patch", "--format=%P%x00%an%x00%ae%x00%B", sha).decode("utf-8").split("\0", 3)
                parents, name, mail, message = fields
                record["author"] = self.github_authors.enrich(sha, mail, author_identity(name, mail))
                record["parents"] = parents.split()
                if sha not in self.eligible:
                    record["reason"] = "blamed_commit_not_in_head_minus_base"
                else:
                    record.update({"category": "own" if sha in self.own else "imported", "confidence": "high",
                                   "reason": "PR_first_parent_commit" if sha in self.own else "merged_side_history_commit"})
                    if len(record["parents"]) > 1:
                        record.update({"confidence": "low", "reason": "blame_points_to_merge_commit; conflict_resolution_or_merge_edits"})
                    elif re.search(r"\bsquash(?:ed|ing)?\b|^co-authored-by:|\(#\d+\)(?:\n|$)", message, re.IGNORECASE | re.MULTILINE):
                        record.update({"confidence": "low", "reason": "possible_squash_or_multi_author_commit_message"})
                    elif not mail.strip():
                        record.update({"confidence": "low", "reason": "Git_author_has_no_email_identity"})
            except (OSError, subprocess.SubprocessError, UnicodeError, ValueError, TypeError):
                pass
            self.commits[sha] = record
        return self.commits[sha]


def summarize_attribution(findings):
    selected = [item for item in findings if item["overlaps_changed_lines"]]
    people = {}
    for item in selected:
        author = item.get("author") or {"id": "unknown", "name": "Unresolved", "github_login": None}
        person = people.setdefault(author["id"], {**author, "aliases": [], "total": 0,
            "by_category": {"own": 0, "imported": 0, "unknown": 0}, "by_confidence": {"high": 0, "low": 0},
            "by_rule": {}, "rules_by_category": {"own": {}, "imported": {}, "unknown": {}}})
        if author["name"] not in person["aliases"]:
            person["aliases"].append(author["name"])
        person["total"] += 1
        person["by_category"][item["category"]] += 1
        person["by_confidence"][item["confidence"]] += 1
        rule = item["rule"]
        person["by_rule"][rule] = person["by_rule"].get(rule, 0) + 1
        rules = person["rules_by_category"][item["category"]]
        rules[rule] = rules.get(rule, 0) + 1
    return {"total_raw_violations": len(findings), "changed_line_violations": len(selected),
            "outside_changed_lines": len(findings) - len(selected),
            "by_category": {**{key: sum(item["category"] == key for item in selected) for key in ("own", "imported", "unknown")},
                            "not_in_scope": len(findings) - len(selected)},
            "by_confidence": {key: sum(item["confidence"] == key for item in selected) for key in ("high", "low")},
            "by_rule": dict(sorted(Counter(item["rule"] for item in selected).items())),
            "people": sorted(people.values(), key=lambda person: person["id"])}


def attribute_violations(root, metadata, changes, violations):
    history = AttributionHistory(root, metadata.get("comparison", {}))
    ranges = {item["path"]: item.get("changed_line_ranges", []) for item in changes if item.get("scanned")}
    findings = []
    for index, violation in enumerate(violations):
        item = {**violation, "violation_index": index,
                "violation_id": sha256(json.dumps([index, finding_key(violation), violation.get("description")], ensure_ascii=False).encode("utf-8")),
                "category": "not_in_scope", "confidence": None, "reason": "outside_changed_lines",
                "author": None, "commit": None, "contributors": []}
        if violation["overlaps_changed_lines"]:
            item.update({"category": "unknown", "confidence": "low"})
            item["reason"] = "incomplete_git_history" if not history.ready else "blame_unavailable_or_incomplete"
            if history.ready:
                blame = history.blame(violation["file"])
                spans = [(max(int(violation["beginline"]), start), min(int(violation["endline"]), stop))
                         for start, stop in ranges.get(violation["file"], [])
                         if int(violation["beginline"]) <= stop and int(violation["endline"]) >= start]
                contributors, missing = {}, False
                for start, stop in spans:
                    for line in range(start, stop + 1):
                        blamed = blame.get(line)
                        if blamed is None:
                            missing = True
                            continue
                        contributor = contributors.setdefault(blamed["commit"], {**history.classify(blamed), "lines": []})
                        contributor["lines"].append(line)
                item["contributors"] = list(contributors.values())
                signatures = {(c["category"], (c.get("author") or {}).get("id")) for c in contributors.values()}
                if not spans or missing or not contributors:
                    item["reason"] = "blame_unavailable_or_incomplete"
                elif len(signatures) != 1:
                    item["reason"] = "finding_spans_multiple_authors_or_provenance_categories"
                else:
                    first = next(iter(contributors.values()))
                    item.update({"category": first["category"], "author": first["author"],
                                 "commit": first["commit"] if len(contributors) == 1 else None,
                                 "confidence": "low" if any(c["confidence"] == "low" for c in contributors.values()) else "high",
                                 "reason": "; ".join(sorted({c["reason"] for c in contributors.values()}))})
        findings.append(item)
    return {"schema_version": SCHEMA_VERSION, "method": "git_blame_and_first_parent_topology",
            "confidence_scope": "recorded_git_provenance_not_verified_original_authorship",
            "repository": metadata.get("run", {}).get("github_repository") or metadata.get("pull_request", {}).get("base_repository"),
            "pull_request": metadata.get("pull_request", {}), "run": metadata.get("run", {}),
            "comparison": metadata.get("comparison", {}), "started_at": metadata.get("started_at"),
            "warnings": sorted(set(history.warnings)), "findings": findings, "summary": summarize_attribution(findings)}


def attribution_markdown(summary):
    lines = ["", "### Attribution on changed lines", "",
             "Low-confidence findings require review and must not be treated as personal scores automatically.",
             "High confidence describes recorded Git provenance; it does not verify the original human author.", "",
             "| Git author | Own | Imported | Unknown | High | Low |", "| --- | ---: | ---: | ---: | ---: | ---: |"]
    for person in summary["people"]:
        categories, confidence = person["by_category"], person["by_confidence"]
        name = person["github_login"] or person["name"]
        lines.append(f"| {markdown(name)} | {categories['own']} | {categories['imported']} | {categories['unknown']} | {confidence['high']} | {confidence['low']} |")
    lines.extend(["", "| Git author | Category | Rule | Count |", "| --- | --- | --- | ---: |"])
    for person in summary["people"]:
        for category, rules in person["rules_by_category"].items():
            for rule, count in sorted(rules.items()):
                lines.append(f"| {markdown(person['github_login'] or person['name'])} | {category} | {markdown(rule)} | {count} |")
    lines.extend(["", f"Outside changed lines: **{summary['outside_changed_lines']}** (retained in raw reports; excluded from personal attribution)."])
    return lines


def finalize(root, output):
    output.mkdir(parents=True, exist_ok=True)
    errors, violations, suppressed, exit_codes = [], [], [], {}
    metadata, changes = {}, []
    try:
        metadata = json.loads((output / "metadata.json").read_text(encoding="utf-8"))
        previous_status = json.loads((output / "status.json").read_text(encoding="utf-8"))
        if previous_status["started_at"] != metadata["started_at"]:
            raise ValueError("Preparation failed; previous run metadata cannot be reused")
        changes = json.loads((output / "changed-files.json").read_text(encoding="utf-8"))
        scan_files = {item["path"]: item for item in changes if item["scanned"]}
        if set(scan_files) != set(metadata["scope"]["files"]) or len(scan_files) != metadata["scope"]["file_count"]:
            raise ValueError("Selected files do not match the recorded scan scope")
        if scan_files:
            started = datetime.fromisoformat(metadata["started_at"]).timestamp()
            for name in ("pmd.json", "pmd.xml", "exit-json.txt", "exit-xml.txt"):
                if (output / name).stat().st_mtime + 0.001 < started:
                    raise ValueError(f"Stale report from an earlier run: {name}")
            for fmt in ("json", "xml"):
                exit_codes[fmt] = int((output / f"exit-{fmt}.txt").read_text().strip())
                if exit_codes[fmt] not in (0, 4):
                    errors.append({"format": fmt, "message": f"PMD exited with {exit_codes[fmt]}"})
            report = json.loads((output / "pmd.json").read_text(encoding="utf-8"))
            errors.extend(report.get("processingErrors", []))
            errors.extend(report.get("configurationErrors", []))
            suppressed = report.get("suppressedViolations", [])
            for file in report["files"]:
                filename = report_path(root, file["filename"])
                if filename not in scan_files:
                    raise ValueError(f"PMD reported a file outside the selected diff: {filename}")
                for finding in file["violations"]:
                    begin = int(finding["beginline"])
                    end = int(finding["endline"])
                    on_diff = any(begin <= stop and end >= start for start, stop in scan_files[filename]["changed_line_ranges"])
                    violations.append({**finding, "file": filename, "overlaps_changed_lines": on_diff})
            xml = ET.parse(output / "pmd.xml").getroot()
            xml_findings = {}
            for file in xml:
                if file.tag.rsplit("}", 1)[-1] != "file":
                    continue
                for node in file:
                    if node.tag.rsplit("}", 1)[-1] == "violation":
                        finding = {**node.attrib, "file": report_path(root, file.attrib["name"])}
                        xml_findings.setdefault(finding_key(finding), []).append(finding)
            for finding in violations:
                matches = xml_findings.get(finding_key(finding), [])
                if not matches:
                    raise ValueError("PMD JSON and XML findings differ")
                # PMD's JSON renderer omits context supplied by its XML renderer.
                context = matches.pop()
                finding.update({key: context[key] for key in ("package", "class", "method", "variable") if key in context})
            if any(xml_findings.values()):
                errors.append({"message": "PMD XML contains findings missing from JSON"})
            for node in xml.iter():
                if node.tag.rsplit("}", 1)[-1] in ("error", "configerror"):
                    errors.append({"format": "xml", "message": ET.tostring(node, encoding="unicode")})
            if any(code == 4 for code in exit_codes.values()) and not violations:
                errors.append({"message": "PMD reported violations but the reports contain none"})
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError) as error:
        errors.append({"message": f"Incomplete or invalid PMD evidence: {error}"})
    attribution = attribute_violations(root, metadata, changes, violations)
    completed = now()
    status = "error" if errors else "failed" if any(v["overlaps_changed_lines"] for v in violations) else "no_java_changes" if metadata.get("scope", {}).get("file_count") == 0 else "passed"
    gating = [v for v in violations if v["overlaps_changed_lines"]]
    counts = {"raw_violations": len(violations), "gating_violations": len(gating),
              "outside_changed_lines": len(violations) - len(gating), "errors": len(errors), "suppressed": len(suppressed),
              "raw_by_rule": dict(sorted(Counter(v["rule"] for v in violations).items())),
              "raw_by_priority": dict(sorted(Counter(str(v["priority"]) for v in violations).items())),
              "raw_by_file": dict(sorted(Counter(v["file"] for v in violations).items())),
              "gating_by_rule": dict(sorted(Counter(v["rule"] for v in gating).items()))}
    duration = (datetime.fromisoformat(completed) - datetime.fromisoformat(metadata["started_at"])).total_seconds() if metadata else None
    result = {"schema_version": SCHEMA_VERSION, "status": status, "gate_scope": CHECK_SCOPE, "gate_scope_description": CHECK_SCOPE_DESCRIPTION, "started_at": metadata.get("started_at"), "completed_at": completed, "duration_seconds": duration, "exit_codes": exit_codes, "file_count": metadata.get("scope", {}).get("file_count"), "counts": counts}
    write_json(output / "status.json", result)
    write_json(output / "violations.json", violations)
    write_json(output / "errors.json", errors)
    write_json(output / "suppressed.json", suppressed)
    attribution.update({"check_status": status, "check_scope": result["gate_scope"], "check_scope_description": CHECK_SCOPE_DESCRIPTION, "check_counts": counts, "completed_at": completed})
    write_json(output / "attribution.json", attribution)
    columns = ("file", "beginline", "endline", "begincolumn", "endcolumn", "rule", "ruleset", "priority", "description", "package", "class", "method", "variable", "externalInfoUrl", "overlaps_changed_lines")
    with (output / "violations.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(columns)
        writer.writerows([csv_cell(v.get(key)) for key in columns] for v in violations)
    lines = ["## PMD PR quality", "", f"Status: **{status}**", "", f"Java files: **{result['file_count']}** | Raw violations: **{len(violations)}** | Errors: **{len(errors)}**", "", f"Gating violations: **{counts['gating_violations']}** | Outside changed lines (informational): **{counts['outside_changed_lines']}**.", "", "The check applies to every unsuppressed finding overlapping changed lines, regardless of attribution or confidence. Findings outside changed lines remain in the reports.", "", "| Rule | Raw count | Gating count |", "| --- | ---: | ---: |"]
    lines.extend(f"| {markdown(rule)} | {count} | {counts['gating_by_rule'].get(rule, 0)} |" for rule, count in counts["raw_by_rule"].items())
    lines.extend(["", "| File | Line | Rule | Priority | Changed lines |", "| --- | ---: | --- | ---: | --- |"])
    lines.extend(f"| {markdown(v['file'])} | {v['beginline']} | {markdown(v['rule'])} | {v['priority']} | {v['overlaps_changed_lines']} |" for v in violations[:100])
    if len(violations) > 100:
        lines.append("\nOnly the first 100 findings are shown here; the artifacts contain every finding.")
    lines.extend(["", "Full metadata, diff, rules, PMD JSON/XML, CSV and logs are archived per PR/run/attempt for 90 days. No Java changes means no PMD invocation."])
    if errors:
        lines.extend(["", "Errors:", ""] + [f"- {markdown(error.get('message', error))}" for error in errors[:20]])
    lines.extend(attribution_markdown(attribution["summary"]))
    if attribution["warnings"]:
        lines.extend(["", "Attribution warnings:", ""] + [f"- {markdown(warning)}" for warning in attribution["warnings"]])
    summary = "\n".join(lines) + "\n"
    (output / "summary.md").write_text(summary, encoding="utf-8")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as handle:
            handle.write(summary)
    # Include integrity hashes for all evidence, except the manifest itself.
    manifest = {path.name: sha256(path.read_bytes()) for path in sorted(output.iterdir()) if path.is_file() and path.name != "manifest.json"}
    write_json(output / "manifest.json", {"schema_version": SCHEMA_VERSION, "sha256": manifest})
    return 1 if status in ("error", "failed") else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("prepare", "finalize"))
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path, default=Path("backend/target/pmd-diff"))
    parser.add_argument("--base")
    parser.add_argument("--head")
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    if args.command == "finalize":
        return finalize(root, output)
    if not args.base or not args.head:
        parser.error("prepare requires --base and --head")
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8")) if os.environ.get("GITHUB_EVENT_PATH") else {}
    prepare(root, output, args.base, args.head, event)
    return 0


if __name__ == "__main__":
    sys.exit(main())
