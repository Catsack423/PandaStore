"""Combine downloaded attribution.json files, selecting the latest run per repo/PR."""

import argparse
from collections import Counter
import csv
from datetime import datetime
import json
from pathlib import Path
import sys

from pmd_diff import CHECK_SCOPE, SCHEMA_VERSION, attribution_markdown, csv_cell, markdown, now, summarize_attribution, write_json


def input_paths(inputs):
    paths = set()
    for source in inputs:
        if source.is_dir():
            found = list(source.rglob("attribution.json"))
            if not found:
                raise ValueError(f"No attribution.json found under {source}")
            paths.update(path.resolve() for path in found)
        else:
            paths.add(source.resolve())
    return sorted(paths)


def run_key(record):
    run = record.get("run", {})
    run_id = str(run.get("github_run_id") or "")
    attempt = str(run.get("github_run_attempt") or "1")
    # Rerunning an older head must not replace a newer run merely because the
    # old run finished later. Run IDs order runs, attempts order reruns.
    started = datetime.fromisoformat(record["started_at"])
    if started.tzinfo is None:
        raise ValueError("started_at must include a timezone")
    return (run_id.isdigit(), int(run_id) if run_id.isdigit() else 0,
            int(attempt) if attempt.isdigit() else 0, started.timestamp())


def normalize(record):
    """Expand legacy records in memory only; downloaded evidence stays unchanged."""
    version = record.get("schema_version")
    if version not in (1, 2):
        raise ValueError("Unsupported attribution schema_version")
    if version == 2:
        return record
    findings = []
    for finding in record["findings"]:
        item = dict(finding)
        if item.get("reason") == "outside_changed_lines" and item.get("overlaps_changed_lines") is False:
            item.update({"category": "not_in_scope", "confidence": None})
        findings.append(item)
    migrated = {**record, "schema_version": SCHEMA_VERSION, "findings": findings}
    if record.get("check_scope") == "all_violations_overlapping_changed_lines":
        migrated["check_scope"] = CHECK_SCOPE
    if "check_counts" in record:
        counts = record["check_counts"]
        migrated["check_counts"] = {
            **{key: value for key, value in counts.items() if key not in ("violations", "on_changed_lines", "by_rule", "by_priority", "by_file")},
            "raw_violations": counts["violations"], "gating_violations": counts["on_changed_lines"],
            "outside_changed_lines": counts["violations"] - counts["on_changed_lines"],
            **{"raw_" + key: counts[key] for key in ("by_rule", "by_priority", "by_file")},
            "gating_by_rule": dict(sorted(Counter(item["rule"] for item in findings if item["overlaps_changed_lines"]).items()))}
    return migrated


def validate(record):
    repo = record.get("repository")
    pr = record.get("pull_request", {}).get("number")
    if not isinstance(repo, str) or not repo or not isinstance(pr, int) or isinstance(pr, bool) or pr < 1:
        raise ValueError("Repository and real PR number are required; local no-PR runs cannot be deduplicated")
    if record.get("check_status") not in ("passed", "failed", "error", "no_java_changes"):
        raise ValueError("Missing or invalid check_status")
    run_key(record)
    if record["pull_request"].get("author") is not None and not isinstance(record["pull_request"]["author"], str):
        raise ValueError("Invalid PR opener")
    if not isinstance(record["findings"], list) or not all(isinstance(warning, str) for warning in record.get("warnings", [])):
        raise ValueError("Invalid findings or warnings")
    for item in record["findings"]:
        if not isinstance(item["overlaps_changed_lines"], bool):
            raise ValueError("Invalid overlaps_changed_lines")
        if item["overlaps_changed_lines"]:
            valid = item["category"] in ("own", "imported", "unknown") and item["confidence"] in ("high", "low")
        else:
            valid = item["category"] == "not_in_scope" and item["confidence"] is None
        if not valid:
            raise ValueError("Invalid attribution category or confidence")
        if not isinstance(item["rule"], str):
            raise ValueError("Invalid rule")
        author = item.get("author")
        if author is not None:
            if not isinstance(author, dict) or not isinstance(author.get("id"), str) or not isinstance(author.get("name"), str):
                raise ValueError("Invalid author identity")
            if "github_login" not in author or (author["github_login"] is not None and not isinstance(author["github_login"], str)):
                raise ValueError("Invalid author login")
    return repo, pr


def aggregate(inputs, output):
    latest, errors = {}, []
    try:
        paths = input_paths(inputs)
    except (OSError, ValueError) as error:
        paths = []
        errors.append({"message": str(error)})
    for path in paths:
        try:
            record = normalize(json.loads(path.read_text(encoding="utf-8")))
            identity = validate(record)
            candidate = (run_key(record), str(path), record)
            if identity not in latest or candidate[:2] > latest[identity][:2]:
                latest[identity] = candidate
        except (OSError, ValueError, KeyError, TypeError, AttributeError) as error:
            errors.append({"file": str(path), "message": str(error)})
    if not latest and not errors:
        errors.append({"message": "No valid PR attribution inputs"})
    prs, findings, authors = [], [], {}
    for (repo, number), (_, source, record) in sorted(latest.items()):
        summary = summarize_attribution(record["findings"])
        pr = {"repository": repo, "number": number, "author": record["pull_request"].get("author"),
              "head_sha": record.get("comparison", {}).get("head_sha"), "run": record.get("run", {}),
              "check_status": record["check_status"], "source": source,
              "check_scope": record.get("check_scope", "unspecified"),
              "raw_violations": summary["total_raw_violations"], "changed_line_violations": summary["changed_line_violations"],
              "outside_changed_lines": summary["outside_changed_lines"],
              "by_category": summary["by_category"], "by_confidence": summary["by_confidence"], "by_rule": summary["by_rule"],
              "attribution_warnings": record.get("warnings", [])}
        prs.append(pr)
        findings.extend(record["findings"])
        author = pr["author"] or "unknown"
        person = authors.setdefault(author, {"github_login": author, "prs": 0,
                                           "by_status": {"passed": 0, "failed": 0, "error": 0, "no_java_changes": 0}})
        person["prs"] += 1
        person["by_status"][pr["check_status"]] += 1
    summary = summarize_attribution(findings)
    report = {"schema_version": SCHEMA_VERSION, "generated_at": now(), "complete": not errors,
              "input_files": len(paths), "selected_prs": len(prs), "superseded_or_duplicate_inputs": max(0, len(paths) - len(errors) - len(prs)),
              "selection_policy": "repository+PR; newest GitHub run_id then attempt; timestamp fallback for offline runs",
              "pull_requests": prs, "pr_authors": sorted(authors.values(), key=lambda person: person["github_login"]),
              "summary": summary, "errors": errors}
    output.mkdir(parents=True, exist_ok=True)
    write_json(output / "summary.json", report)
    lines = ["# PMD cross-PR summary", "", f"Complete: **{not errors}** | Selected PRs: **{len(prs)}**", "",
             "Exactly one latest result is counted per repository/PR. PR opener and Git finding author are separate identities.", "",
             "| Repository | PR | PR opener | Check | Raw | Changed lines | Low confidence |",
             "| --- | ---: | --- | --- | --- | ---: | ---: |"]
    lines.extend(f"| {markdown(pr['repository'])} | {pr['number']} | {markdown(pr['author'])} | {pr['check_status']} | {pr['raw_violations']} | {pr['changed_line_violations']} | {pr['by_confidence']['low']} |" for pr in prs)
    lines.extend(["", "| PR opener | PRs | Passed | Failed | Error | No Java changes |", "| --- | ---: | ---: | ---: | ---: | ---: |"])
    for author in report["pr_authors"]:
        statuses = author["by_status"]
        lines.append(f"| {markdown(author['github_login'])} | {author['prs']} | {statuses['passed']} | {statuses['failed']} | {statuses['error']} | {statuses['no_java_changes']} |")
    lines.extend(attribution_markdown(summary))
    lines.extend(["", "| Rule | Changed-line violations |", "| --- | ---: |"])
    lines.extend(f"| {markdown(rule)} | {count} |" for rule, count in summary["by_rule"].items())
    warnings = [warning for pr in prs for warning in pr["attribution_warnings"]]
    if warnings:
        lines.extend(["", "Attribution warnings:", ""] + [f"- {markdown(warning)}" for warning in sorted(set(warnings))])
    if errors:
        lines.extend(["", "Input errors (report is incomplete):", ""] + [f"- {markdown(error)}" for error in errors])
    (output / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    with (output / "people.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(("author_id", "name", "github_login", "category", "confidence", "rule", "violation_count"))
        identities = {person["id"]: person for person in summary["people"]}
        groups = Counter(((item.get("author") or {}).get("id", "unknown"), item["category"], item["confidence"], item["rule"])
                         for item in findings if item["overlaps_changed_lines"])
        for (identity, category, confidence, rule), count in sorted(groups.items()):
            person = identities[identity]
            writer.writerow([csv_cell(value) for value in (identity, person["name"], person["github_login"], category, confidence, rule, count)])
    with (output / "pull-requests.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(("repository", "pr", "pr_opener", "check_status", "raw_violations", "changed_line_violations", "own", "imported", "unknown", "low", "not_in_scope"))
        for pr in prs:
            writer.writerow([csv_cell(value) for value in (pr["repository"], pr["number"], pr["author"], pr["check_status"],
                pr["raw_violations"], pr["changed_line_violations"], *(pr["by_category"][key] for key in ("own", "imported", "unknown")), pr["by_confidence"]["low"], pr["by_category"]["not_in_scope"])])
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inputs", nargs="+", type=Path, help="Downloaded attribution.json files or directories containing them")
    parser.add_argument("--output", type=Path, default=Path("backend/target/pmd-summary"))
    args = parser.parse_args()
    report = aggregate(args.inputs, args.output)
    print(f"Selected {report['selected_prs']} latest PR results; complete={report['complete']}; output={args.output}")
    return 0 if report["complete"] else 1


if __name__ == "__main__":
    sys.exit(main())
