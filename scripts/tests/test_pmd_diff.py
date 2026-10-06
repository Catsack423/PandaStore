"""Exercise the PR ownership boundary and failure states using actual git diffs."""

import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import MagicMock, patch
import xml.etree.ElementTree as ET

SCRIPT = Path(__file__).resolve().parents[1] / "pmd_diff.py"
SPEC = importlib.util.spec_from_file_location("pmd_diff", SCRIPT)
pmd = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(pmd)


class PmdDiffTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / "backend/target/pmd-diff"
        self.env = patch.dict(os.environ, {"GITHUB_OUTPUT": "", "GITHUB_STEP_SUMMARY": "", "PMD_GITHUB_TOKEN": ""})
        self.env.start()
        self.addCleanup(self.env.stop)
        self.git("init", "-b", "main")
        self.git("config", "user.name", "PMD test")
        self.git("config", "user.email", "pmd-test@example.invalid")
        self.git("config", "core.autocrlf", "false")
        self.write("config/pmd/ruleset.xml", "<ruleset />\n")
        self.write("backend/src/main/java/Unchanged.java", "class Unchanged {}\n")
        self.write("backend/src/main/java/Modified.java", "class Modified {\n    int count;\n}\n")
        self.write("backend/src/main/java/Rename.java", "class Rename {\n    int size;\n    int total;\n}\n")
        self.write("backend/src/main/java/Delete.java", "class Delete {}\n")
        self.base = self.commit("base")
        self.git("checkout", "-b", "feature")

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], stderr=subprocess.PIPE).decode().strip()

    def write(self, path, content):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content, encoding="utf-8", newline="\n")

    def commit(self, message):
        self.git("add", ".")
        self.git("commit", "-m", message)
        return self.git("rev-parse", "HEAD")

    def prepare(self, head=None, base=None):
        return pmd.prepare(self.root, self.output, base or self.base, head or self.git("rev-parse", "HEAD"))

    def read(self, name):
        return json.loads((self.output / name).read_text(encoding="utf-8"))

    def reports(self, violations=None, processing_errors=None, code=0, file_violations=None):
        for fmt in ("json", "xml"):
            (self.output / f"exit-{fmt}.txt").write_text(str(code))
        violations = violations or []
        groups = file_violations if file_violations is not None else {"backend/src/main/java/Modified.java": violations}
        data = {"files": [{"filename": filename, "violations": findings} for filename, findings in groups.items()],
                "processingErrors": processing_errors or [], "configurationErrors": []}
        pmd.write_json(self.output / "pmd.json", data)
        xml = ET.Element("pmd", {"xmlns": "http://pmd.sourceforge.net/report/2.0.0"})
        for filename, findings in groups.items():
            file = ET.SubElement(xml, "file", {"name": filename})
            for finding in findings:
                attributes = {key: str(finding[key]) for key in ("rule", "beginline", "endline", "begincolumn", "endcolumn") if key in finding}
                ET.SubElement(file, "violation", {**attributes, "class": "Modified", "method": "example"})
        ET.ElementTree(xml).write(self.output / "pmd.xml", encoding="utf-8")

    def changed(self):
        self.write("backend/src/main/java/Modified.java", "class Modified {\n    int changed;\n}\n")
        return self.commit("edit Java")

    def test_select_only_pr_java_including_renames_and_spaces(self):
        self.changed()
        self.write("backend/src/main/java/Added File.java", "class Added {}\n")
        self.write("backend/src/test/java/Test.java", "class Test {}\n")
        self.write("frontend/src/Other.java", "class Other {}\n")
        self.git("mv", "backend/src/main/java/Rename.java", "backend/src/main/java/Renamed.java")
        self.git("rm", "backend/src/main/java/Delete.java")
        head = self.commit("rename, delete and add")
        metadata = self.prepare(head)
        self.assertEqual(set(metadata["scope"]["files"]), {
            "backend/src/main/java/Modified.java", "backend/src/main/java/Added File.java", "backend/src/main/java/Renamed.java"})
        changes = {item["path"]: item for item in self.read("changed-files.json")}
        self.assertEqual(changes["backend/src/main/java/Modified.java"]["changed_line_ranges"], [[2, 2]])
        self.assertEqual(changes["backend/src/main/java/Renamed.java"]["changed_line_ranges"], [])
        self.assertEqual(changes["backend/src/main/java/Renamed.java"]["added_lines"], 0)
        self.assertEqual(len(self.read("commits.json")), 2)
        self.assertFalse(changes["backend/src/main/java/Delete.java"]["scanned"])

    def test_develop_advances_after_fork_and_is_not_used_as_two_dot_base(self):
        self.git("branch", "-m", "main", "develop")
        head = self.changed()
        self.git("checkout", "develop")
        self.write("backend/src/main/java/BaseOnly.java", "class BaseOnly {}\n")
        self.write("backend/src/main/java/Modified.java", "class Modified {\n    int count;\n    int developOnly;\n}\n")
        base = self.commit("advance target branch")
        self.git("checkout", "feature")
        metadata = self.prepare(head, base)
        self.assertEqual(metadata["comparison"]["merge_base_sha"], self.base)
        self.assertEqual(metadata["scope"]["files"], ["backend/src/main/java/Modified.java"])
        changed = next(item for item in self.read("changed-files.json") if item["scanned"])
        self.assertEqual((changed["added_lines"], changed["deleted_lines"], changed["changed_line_ranges"]), (1, 1, [[2, 2]]))

    def test_no_java_is_successful_with_explicit_status(self):
        self.write("README.md", "no Java\n")
        self.prepare(self.commit("docs"))
        self.assertEqual(pmd.finalize(self.root, self.output), 0)
        self.assertEqual(self.read("status.json")["status"], "no_java_changes")
        self.assertEqual((self.output / "java.diff.patch").read_bytes(), b"")

    def test_deleted_java_only_is_not_scanned(self):
        self.git("rm", "backend/src/main/java/Delete.java")
        self.prepare(self.commit("delete"))
        self.assertEqual(pmd.finalize(self.root, self.output), 0)
        self.assertIn("Delete.java", (self.output / "java.diff.patch").read_text())

    def test_outside_changed_lines_remain_raw_but_do_not_fail_approved_gate(self):
        self.prepare(self.changed())
        finding = {"beginline": 1, "endline": 1, "rule": "Rule", "ruleset": "Code Style", "priority": 3, "description": "=unsafe formula"}
        self.reports([finding], code=4)
        self.assertEqual(pmd.finalize(self.root, self.output), 0)
        self.assertEqual(self.read("status.json")["status"], "passed")
        self.assertEqual(self.read("status.json")["counts"]["gating_violations"], 0)
        self.assertEqual(self.read("violations.json")[0]["method"], "example")
        self.assertIn("'=unsafe formula", (self.output / "violations.csv").read_text(encoding="utf-8-sig"))
        attribution = self.read("attribution.json")
        self.assertEqual(attribution["schema_version"], 2)
        self.assertEqual(attribution["check_scope"], "violations_overlapping_changed_lines")
        self.assertIn("informational only", attribution["check_scope_description"])
        item = attribution["findings"][0]
        self.assertEqual((item["category"], item["confidence"], item["reason"]), ("not_in_scope", None, "outside_changed_lines"))
        self.assertEqual(attribution["summary"]["by_category"], {"own": 0, "imported": 0, "unknown": 0, "not_in_scope": 1})
        self.assertEqual(attribution["summary"]["by_confidence"], {"high": 0, "low": 0})
        self.assertEqual(attribution["summary"]["people"], [])
        self.assertEqual(attribution["check_counts"]["gating_by_rule"], {})

    def test_raw_gating_and_outside_counts_partition_all_findings(self):
        self.prepare(self.changed())
        self.reports([self.finding(1, rule="OutsideRule"), self.finding(2, rule="GatingRule"),
                      self.finding(3, rule="OutsideRule")], code=4)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        attribution = self.read("attribution.json")
        counts = attribution["check_counts"]
        self.assertEqual(counts["raw_violations"], counts["gating_violations"] + counts["outside_changed_lines"])
        self.assertEqual((counts["raw_violations"], counts["gating_violations"], counts["outside_changed_lines"]), (3, 1, 2))
        self.assertEqual(counts["raw_by_rule"], {"GatingRule": 1, "OutsideRule": 2})
        self.assertEqual(counts["gating_by_rule"], {"GatingRule": 1})
        self.assertEqual(counts["raw_by_priority"], {"3": 3})
        self.assertEqual(counts["raw_by_file"], {"backend/src/main/java/Modified.java": 3})
        self.assertEqual(counts, self.read("status.json")["counts"])
        self.assertEqual(len(self.read("violations.json")), 3)
        self.assertEqual(attribution["summary"]["by_category"], {"own": 1, "imported": 0, "unknown": 0, "not_in_scope": 2})
        for old in ("violations", "on_changed_lines", "by_rule", "by_priority", "by_file"):
            self.assertNotIn(old, counts)

    def finding(self, line, end=None, rule="ExampleRule"):
        return {"beginline": line, "endline": end or line, "rule": rule, "ruleset": "Code Style", "priority": 3, "description": "example"}

    def attribution(self, metadata, findings):
        changes = self.read("changed-files.json")
        return pmd.attribute_violations(self.root, metadata, changes, findings)

    def test_merge_develop_into_feature_excludes_develop_violations(self):
        self.git("branch", "-m", "main", "develop")
        self.changed()
        self.git("checkout", "develop")
        self.write("backend/src/main/java/FromDevelop.java", "class FromDevelop {}\n")
        base = self.commit("develop Java")
        self.git("checkout", "feature")
        self.git("merge", "--no-ff", "develop", "-m", "Merge develop")
        metadata = self.prepare(base=base)
        self.assertEqual(metadata["comparison"]["merge_base_sha"], base)
        self.assertEqual(metadata["scope"]["files"], ["backend/src/main/java/Modified.java"])
        self.reports([self.finding(2)], code=4)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        attribution = self.read("attribution.json")
        self.assertEqual(attribution["summary"]["by_category"], {"own": 1, "imported": 0, "unknown": 0, "not_in_scope": 0})
        self.assertNotIn("FromDevelop.java", (self.output / "java.diff.patch").read_text())

    def import_other_then_continue(self):
        self.git("checkout", "-b", "other", self.base)
        self.git("config", "user.name", "Other person")
        self.git("config", "user.email", "other@example.invalid")
        filename = "backend/src/main/java/Borrowed.java"
        self.write(filename, "class Borrowed {\n    int imported;\n}\n")
        imported = self.commit("other work")
        self.git("checkout", "feature")
        self.git("config", "user.name", "PR contributor")
        self.git("config", "user.email", "owner@example.invalid")
        self.changed()
        self.git("merge", "--no-ff", "other", "-m", "Merge other feature")
        self.write(filename, "class Borrowed {\n    int imported;\n    int own;\n}\n")
        own = self.commit("continue imported file")
        return filename, imported, own

    def test_merged_other_feature_and_own_followup_get_separate_people(self):
        filename, imported, own = self.import_other_then_continue()
        self.prepare()
        self.reports(code=4, file_violations={filename: [self.finding(2), self.finding(3)]})
        original_json = (self.output / "pmd.json").read_bytes()
        original_xml = (self.output / "pmd.xml").read_bytes()
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        attribution = self.read("attribution.json")
        by_line = {item["beginline"]: item for item in attribution["findings"]}
        self.assertEqual((by_line[2]["category"], by_line[2]["commit"], by_line[2]["author"]["name"]), ("imported", imported, "Other person"))
        self.assertEqual((by_line[3]["category"], by_line[3]["commit"], by_line[3]["author"]["name"]), ("own", own, "PR contributor"))
        self.assertEqual(attribution["summary"]["by_category"], {"own": 1, "imported": 1, "unknown": 0, "not_in_scope": 0})
        self.assertEqual(attribution["summary"]["by_confidence"], {"high": 2, "low": 0})
        self.assertEqual(len(attribution["summary"]["people"]), 2)
        self.assertIn("Other person", (self.output / "summary.md").read_text())
        self.assertEqual((self.output / "pmd.json").read_bytes(), original_json)
        self.assertEqual((self.output / "pmd.xml").read_bytes(), original_xml)

    def test_finding_spanning_multiple_people_is_unknown_and_counted_once(self):
        filename, _, _ = self.import_other_then_continue()
        self.prepare()
        self.reports(code=4, file_violations={filename: [self.finding(2, 3)]})
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        attribution = self.read("attribution.json")
        item = attribution["findings"][0]
        self.assertEqual((item["category"], item["confidence"], item["author"]), ("unknown", "low", None))
        self.assertEqual(len(item["contributors"]), 2)
        self.assertEqual(attribution["summary"]["people"][0]["total"], 1)

    def test_real_squash_commit_has_low_confidence(self):
        self.git("checkout", "-b", "other", self.base)
        self.changed()
        self.git("checkout", "feature")
        self.git("merge", "--squash", "other")
        squash = self.commit("Squashed commit of the following:\n\ncombined other work")
        self.prepare()
        self.reports([self.finding(2)], code=4)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        item = self.read("attribution.json")["findings"][0]
        self.assertEqual(item["commit"], squash)
        self.assertEqual(item["confidence"], "low")
        self.assertIn("squash", item["reason"])

    def test_manual_merge_conflict_resolution_has_low_confidence(self):
        self.git("checkout", "-b", "other", self.base)
        self.write("backend/src/main/java/Modified.java", "class Modified {\n    int other;\n}\n")
        self.commit("other change")
        self.git("checkout", "feature")
        self.changed()
        with self.assertRaises(subprocess.CalledProcessError):
            self.git("merge", "--no-ff", "other", "-m", "Merge other")
        self.write("backend/src/main/java/Modified.java", "class Modified {\n    int resolved;\n}\n")
        merge = self.commit("Resolve merge conflict")
        self.prepare()
        self.reports([self.finding(2)], code=4)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        item = self.read("attribution.json")["findings"][0]
        self.assertEqual(item["commit"], merge)
        self.assertEqual((item["category"], item["confidence"]), ("own", "low"))
        self.assertIn("merge_commit", item["reason"])

    def test_missing_blame_commit_is_unknown_low_without_scan_error(self):
        self.prepare(self.changed())
        self.reports([self.finding(2)], code=4)
        with patch.object(pmd.AttributionHistory, "blame", return_value={2: {"commit": "f" * 40, "line": 2}}):
            self.assertEqual(pmd.finalize(self.root, self.output), 1)
        item = self.read("attribution.json")["findings"][0]
        self.assertEqual((item["category"], item["confidence"], item["reason"]), ("unknown", "low", "commit_unavailable"))
        self.assertEqual(self.read("status.json")["counts"]["errors"], 0)

    def test_shallow_history_is_unknown_low(self):
        metadata = self.prepare(self.changed())
        cloned = self.root / "shallow-copy"
        self.git("clone", "--depth", "1", self.root.as_uri(), str(cloned))
        findings = [{**self.finding(2), "file": "backend/src/main/java/Modified.java", "overlaps_changed_lines": True}]
        attribution = pmd.attribute_violations(cloned, metadata, self.read("changed-files.json"), findings)
        item = attribution["findings"][0]
        self.assertEqual((item["category"], item["confidence"], item["reason"]), ("unknown", "low", "incomplete_git_history"))
        self.assertTrue(attribution["warnings"])

    def test_blame_failure_and_missing_git_degrade_to_unknown(self):
        self.prepare(self.changed())
        self.reports([self.finding(2)], code=4)
        original = pmd.git
        def failing_blame(root, *args):
            if "blame" in args:
                raise subprocess.CalledProcessError(128, "git blame")
            return original(root, *args)
        with patch.object(pmd, "git", side_effect=failing_blame):
            self.assertEqual(pmd.finalize(self.root, self.output), 1)
        self.assertEqual(self.read("attribution.json")["findings"][0]["category"], "unknown")
        with patch.object(pmd, "git", side_effect=FileNotFoundError("git unavailable")):
            self.assertEqual(pmd.finalize(self.root, self.output), 1)
        self.assertEqual(self.read("attribution.json")["findings"][0]["confidence"], "low")

    def test_noreply_login_and_same_name_different_email_identity(self):
        one = pmd.author_identity("Same Name", "<one@example.invalid>")
        two = pmd.author_identity("Same Name", "<two@example.invalid>")
        self.assertNotEqual(one["id"], two["id"])
        self.assertNotIn("@", one["id"])
        self.assertEqual(pmd.author_identity("Name", "<123+Contributor@users.noreply.github.com>")["id"], "github:contributor")
        for mail in ("123+Contributor@users.noreply.github.com", "Contributor@users.noreply.github.com"):
            author = pmd.author_identity("Different Git name", mail)
            self.assertEqual((author["github_login"], author["github_login_source"]), ("contributor", "github_noreply_email"))
        for mail in ("person@example.invalid", "", "invalid+name@users.noreply.github.com"):
            self.assertIsNone(pmd.author_identity("contributor", mail)["github_login"])

    def api_environment(self):
        return patch.dict(os.environ, {"GITHUB_ACTIONS": "true", "GITHUB_REPOSITORY": "team/PandaStore",
                                      "GITHUB_API_URL": "https://api.github.com", "PMD_GITHUB_TOKEN": "test-only-token"})

    def api_opener(self, payload):
        opener = MagicMock()
        opener.open.return_value.__enter__.return_value.read.return_value = json.dumps(payload).encode()
        return opener

    def test_github_commit_api_enriches_exact_author_without_storing_email_or_token(self):
        head = self.changed()
        self.prepare(head)
        self.reports([self.finding(2)], code=4)
        payload = {"sha": head, "commit": {"author": {"email": "pmd-test@example.invalid"}},
                   "author": {"login": "actual-author"}, "committer": {"login": "someone-else"}}
        opener = self.api_opener(payload)
        with self.api_environment(), patch.object(pmd.urllib.request, "build_opener", return_value=opener):
            self.assertEqual(pmd.finalize(self.root, self.output), 1)
        author = self.read("attribution.json")["findings"][0]["author"]
        self.assertEqual((author["github_login"], author["github_login_source"]), ("actual-author", "github_commit_api"))
        self.assertTrue(author["id"].startswith("git-email-sha256:"))
        self.assertEqual(opener.open.call_count, 1)
        for filename in ("attribution.json", "metadata.json", "commits.json", "summary.md"):
            evidence = (self.output / filename).read_text(encoding="utf-8")
            self.assertNotIn("pmd-test@example.invalid", evidence)
            self.assertNotIn("test-only-token", evidence)

    def test_github_lookup_cannot_guess_from_opener_committer_or_mismatched_commit(self):
        sha, mail = "a" * 40, "someone@example.invalid"
        author = pmd.author_identity("PR-opener", mail)
        good = {"sha": sha, "commit": {"author": {"email": mail}}, "author": {"login": "actual-author"}}
        cases = [{**good, "sha": "b" * 40}, {**good, "author": None, "committer": {"login": "PR-opener"}},
                 {**good, "commit": {"author": {"email": "different@example.invalid"}}}]
        for payload in cases:
            with self.subTest(payload=payload), self.api_environment(), patch.object(pmd.urllib.request, "build_opener", return_value=self.api_opener(payload)):
                self.assertIsNone(pmd.GithubAuthors([]).enrich(sha, mail, author)["github_login"])
        opener = MagicMock()
        opener.open.side_effect = OSError("network unavailable")
        warnings = []
        with self.api_environment(), patch.object(pmd.urllib.request, "build_opener", return_value=opener):
            resolver = pmd.GithubAuthors(warnings)
            self.assertIsNone(resolver.enrich(sha, mail, author)["github_login"])
            self.assertIsNone(resolver.enrich("b" * 40, mail, author)["github_login"])
            self.assertEqual(opener.open.call_count, 1)
        self.assertTrue(warnings)

    def test_github_lookup_failure_keeps_noreply_and_does_not_affect_gate(self):
        self.git("config", "user.email", "123+Contributor@users.noreply.github.com")
        self.prepare(self.changed())
        self.reports([self.finding(2)], code=4)
        opener = MagicMock()
        opener.open.side_effect = OSError("network unavailable")
        with self.api_environment(), patch.object(pmd.urllib.request, "build_opener", return_value=opener):
            self.assertEqual(pmd.finalize(self.root, self.output), 1)
        attribution = self.read("attribution.json")
        author = attribution["findings"][0]["author"]
        self.assertEqual((author["github_login"], author["github_login_source"]), ("contributor", "github_noreply_email"))
        self.assertEqual(attribution["check_status"], "failed")
        self.assertEqual(attribution["check_counts"]["errors"], 0)
        self.assertTrue(attribution["warnings"])

    def test_clean_scan_passes(self):
        self.prepare(self.changed())
        self.reports()
        self.assertEqual(pmd.finalize(self.root, self.output), 0)
        self.assertEqual(self.read("status.json")["status"], "passed")
        manifest = self.read("manifest.json")["sha256"]
        self.assertEqual(manifest["pmd.json"], pmd.sha256((self.output / "pmd.json").read_bytes()))

    def test_parser_errors_fail(self):
        self.prepare(self.changed())
        self.reports(processing_errors=[{"message": "Parse failed"}], code=5)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        self.assertEqual(self.read("status.json")["status"], "error")

    def test_missing_report_or_install_failure_cannot_pass(self):
        self.prepare(self.changed())
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        self.assertEqual(self.read("status.json")["status"], "error")

    def test_stale_report_cannot_pass_on_local_rerun(self):
        self.prepare(self.changed())
        self.reports()
        for fmt in ("json", "xml"):
            os.utime(self.output / f"pmd.{fmt}", (1, 1))
        self.assertEqual(pmd.finalize(self.root, self.output), 1)
        self.assertIn("Stale report", self.read("errors.json")[0]["message"])

    def test_failed_prepare_cannot_reuse_previous_clean_result(self):
        head = self.changed()
        self.prepare(head)
        self.reports()
        self.assertEqual(pmd.finalize(self.root, self.output), 0)
        self.write("backend/src/main/java/Modified.java", "local changes\n")
        with self.assertRaisesRegex(ValueError, "Working file differs"):
            self.prepare(head)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)

    def test_mismatched_xml_and_json_cannot_pass(self):
        self.prepare(self.changed())
        self.reports()
        (self.output / "pmd.xml").write_text("<pmd><file><violation /></file></pmd>")
        self.assertEqual(pmd.finalize(self.root, self.output), 1)

    def test_unexpected_reported_file_cannot_pass(self):
        self.prepare(self.changed())
        self.reports()
        data = self.read("pmd.json")
        data["files"][0]["filename"] = "backend/src/main/java/Unchanged.java"
        pmd.write_json(self.output / "pmd.json", data)
        self.assertEqual(pmd.finalize(self.root, self.output), 1)

    def test_wrong_checkout_or_modified_worktree_is_rejected(self):
        head = self.changed()
        with self.assertRaisesRegex(ValueError, "Checkout must match"):
            self.prepare(self.base)
        self.write("backend/src/main/java/Modified.java", "different content\n")
        with self.assertRaisesRegex(ValueError, "Working file differs"):
            self.prepare(head)

    def test_crlf_checkout_matches_git_normalized_blob(self):
        self.git("config", "core.autocrlf", "true")
        head = self.changed()
        source = self.root / "backend/src/main/java/Modified.java"
        source.write_bytes(source.read_text(encoding="utf-8").replace("\n", "\r\n").encode("utf-8"))
        self.assertEqual(self.prepare(head)["scope"]["file_count"], 1)

    def test_newline_filename_is_rejected_instead_of_skipped(self):
        if os.name == "nt":
            # Windows cannot create newline filenames; test the collector using
            # git's actual NUL-delimited response shape instead.
            original = pmd.git
            def response(root, *args):
                if args[:2] == ("diff", "--name-status"):
                    return b"A\0backend/src/main/java/New\nFile.java\0"
                if args[:2] == ("diff", "--numstat"):
                    return b"1\t0\tbackend/src/main/java/New\\nFile.java\n"
                return original(root, *args)
            with patch.object(pmd, "git", side_effect=response):
                with self.assertRaisesRegex(ValueError, "cannot safely represent"):
                    self.prepare()
        else:
            self.write("backend/src/main/java/New\nFile.java", "class New {}\n")
            with self.assertRaisesRegex(ValueError, "cannot safely represent"):
                self.prepare(self.commit("odd filename"))

    def test_event_metadata_excludes_tokens_and_raw_payload(self):
        event = {"token": "never store", "pull_request": {"number": 42, "body": "not collected", "user": {"login": "owner"}, "head": {"ref": "feature", "repo": None}}}
        result = pmd.pr_context(event)
        self.assertEqual(result["author"], "owner")
        self.assertNotIn("token", result)
        self.assertNotIn("body", result)


if __name__ == "__main__":
    unittest.main()
