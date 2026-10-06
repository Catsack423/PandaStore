"""Test offline aggregation without counting reruns or imported authors twice."""

from copy import deepcopy
import csv
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import pmd_aggregate as aggregate


class PmdAggregateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / "summary"

    def record(self, pr=1, run=100, attempt=1, category="own", confidence="high", name="Contributor", repo="team/PandaStore", status="failed"):
        return {"schema_version": 1, "repository": repo, "pull_request": {"number": pr, "author": "PR-opener"},
                "run": {"github_run_id": str(run), "github_run_attempt": str(attempt)},
                "started_at": "2026-10-06T00:00:00+00:00", "completed_at": "2026-10-06T00:01:00+00:00",
                "check_status": status, "comparison": {"head_sha": "a" * 40}, "warnings": [],
                "findings": [{"overlaps_changed_lines": True, "category": category, "confidence": confidence, "rule": "ExampleRule",
                              "author": {"id": "git-person:" + name, "name": name, "github_login": None}}]}

    def save(self, name, record):
        path = self.root / name / "attribution.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(record), encoding="utf-8")
        return path

    def test_latest_run_then_attempt_and_one_result_per_pr(self):
        self.save("old", self.record(run=100))
        newest = self.record(run=101, attempt=2, status="passed")
        newest["findings"] = []
        self.save("new-attempt-1", self.record(run=101, attempt=1))
        self.save("new-attempt-2", newest)
        old_rerun = self.record(run=100, attempt=9)
        old_rerun["started_at"] = "2026-10-07T00:00:00+00:00"
        self.save("old-rerun-finishes-later", old_rerun)
        self.save("other-pr", self.record(pr=2, run=102, category="imported", name="Other contributor"))
        report = aggregate.aggregate([self.root], self.output)
        self.assertTrue(report["complete"])
        self.assertEqual(report["selected_prs"], 2)
        self.assertEqual(report["superseded_or_duplicate_inputs"], 3)
        self.assertEqual(report["pull_requests"][0]["check_status"], "passed")
        self.assertEqual(report["summary"]["by_category"], {"own": 0, "imported": 1, "unknown": 0, "not_in_scope": 0})
        self.assertEqual(report["pr_authors"][0]["by_status"]["passed"], 1)
        self.assertEqual(report["pr_authors"][0]["by_status"]["failed"], 1)
        self.assertEqual(report["summary"]["people"][0]["name"], "Other contributor")

    def test_same_pr_number_in_different_repositories_is_not_deduplicated(self):
        paths = [self.save("a", self.record()), self.save("b", self.record(repo="another/PandaStore"))]
        report = aggregate.aggregate(paths + paths, self.output)
        self.assertEqual(report["selected_prs"], 2)
        self.assertEqual(report["input_files"], 2)
        self.assertEqual(report["summary"]["people"][0]["total"], 2)

    def test_low_confidence_unknown_and_raw_outside_diff_are_preserved(self):
        record = self.record(category="unknown", confidence="low")
        record["findings"][0]["author"] = None
        outside = deepcopy(record["findings"][0])
        outside["overlaps_changed_lines"] = False
        outside["reason"] = "outside_changed_lines"
        record["findings"].append(outside)
        record["warnings"] = ["incomplete_git_history"]
        report = aggregate.aggregate([self.save("unknown", record)], self.output)
        self.assertEqual(report["summary"]["total_raw_violations"], 2)
        self.assertEqual(report["summary"]["changed_line_violations"], 1)
        self.assertEqual(report["summary"]["people"][0]["id"], "unknown")
        self.assertEqual(report["summary"]["by_category"], {"own": 0, "imported": 0, "unknown": 1, "not_in_scope": 1})
        self.assertEqual(report["summary"]["by_confidence"], {"high": 0, "low": 1})
        self.assertIn("must not be treated as personal scores", (self.output / "summary.md").read_text())
        self.assertIn("incomplete_git_history", (self.output / "summary.md").read_text())

    def test_schema_one_and_two_produce_equivalent_summaries_without_rewriting_inputs(self):
        legacy = self.record(category="unknown", confidence="low")
        legacy["findings"][0].update({"author": None, "reason": "commit_unavailable"})
        outside = deepcopy(legacy["findings"][0])
        outside.update({"overlaps_changed_lines": False, "reason": "outside_changed_lines"})
        legacy["findings"].append(outside)
        legacy["check_scope"] = "all_violations_overlapping_changed_lines"
        legacy["check_counts"] = {"violations": 2, "on_changed_lines": 1, "errors": 0, "suppressed": 0,
                                  "by_rule": {"ExampleRule": 2}, "by_priority": {"3": 2}, "by_file": {"Example.java": 2}}
        current = deepcopy(legacy)
        current["schema_version"] = 2
        current["check_scope"] = "violations_overlapping_changed_lines"
        current["findings"][1].update({"category": "not_in_scope", "confidence": None})
        current["check_counts"] = {"raw_violations": 2, "gating_violations": 1, "outside_changed_lines": 1,
                                   "errors": 0, "suppressed": 0, "raw_by_rule": {"ExampleRule": 2},
                                   "raw_by_priority": {"3": 2}, "raw_by_file": {"Example.java": 2},
                                   "gating_by_rule": {"ExampleRule": 1}}
        legacy_path, current_path = self.save("legacy", legacy), self.save("current", current)
        original = legacy_path.read_bytes()
        normalized = aggregate.normalize(legacy)
        self.assertEqual(normalized, current)
        self.assertEqual(legacy["findings"][1]["category"], "unknown")
        old_report = aggregate.aggregate([legacy_path], self.output)
        new_report = aggregate.aggregate([current_path], self.output)
        self.assertTrue(old_report["complete"] and new_report["complete"])
        self.assertEqual(old_report["summary"], new_report["summary"])
        self.assertEqual(old_report["schema_version"], 2)
        old_pr, new_pr = deepcopy(old_report["pull_requests"][0]), deepcopy(new_report["pull_requests"][0])
        old_pr.pop("source")
        new_pr.pop("source")
        self.assertEqual(old_pr, new_pr)
        self.assertEqual(legacy_path.read_bytes(), original)
        current["run"]["github_run_id"] = "101"
        self.save("current", current)
        combined = aggregate.aggregate([legacy_path, current_path], self.output)
        self.assertEqual(combined["selected_prs"], 1)
        self.assertEqual(combined["summary"], new_report["summary"])

    def test_schema_two_rejects_outside_findings_disguised_as_unknown(self):
        bad = self.record(category="unknown", confidence="low")
        bad["schema_version"] = 2
        bad["findings"][0]["overlaps_changed_lines"] = False
        report = aggregate.aggregate([self.save("invalid-scope", bad)], self.output)
        self.assertFalse(report["complete"])
        self.assertEqual(report["selected_prs"], 0)

    def test_schema_two_outside_only_has_no_people_or_confidence_counts(self):
        record = self.record(status="passed")
        record["schema_version"] = 2
        record["findings"][0].update({"overlaps_changed_lines": False, "category": "not_in_scope", "confidence": None,
                                      "reason": "outside_changed_lines", "author": None})
        report = aggregate.aggregate([self.save("outside-only", record)], self.output)
        self.assertTrue(report["complete"])
        self.assertEqual(report["summary"]["by_category"], {"own": 0, "imported": 0, "unknown": 0, "not_in_scope": 1})
        self.assertEqual(report["summary"]["by_confidence"], {"high": 0, "low": 0})
        self.assertEqual(report["summary"]["people"], [])
        self.assertEqual(report["pull_requests"][0]["check_status"], "passed")
        with (self.output / "people.csv").open(encoding="utf-8-sig", newline="") as handle:
            self.assertEqual(list(csv.DictReader(handle)), [])
        with (self.output / "pull-requests.csv").open(encoding="utf-8-sig", newline="") as handle:
            row = next(csv.DictReader(handle))
            self.assertEqual((row["unknown"], row["not_in_scope"]), ("0", "1"))

    def test_offline_timestamp_fallback(self):
        old, new = self.record(), self.record(status="passed")
        old["run"], new["run"] = {}, {}
        new["started_at"] = "2026-10-06T01:00:00+00:00"
        new["findings"] = []
        report = aggregate.aggregate([self.save("old", old), self.save("new", new)], self.output)
        self.assertEqual(report["pull_requests"][0]["check_status"], "passed")

    def test_invalid_local_no_pr_run_reports_partial_summary_and_nonzero_cli(self):
        valid = self.save("valid", self.record())
        invalid = self.record()
        invalid["pull_request"]["number"] = None
        bad = self.save("local", invalid)
        report = aggregate.aggregate([valid, bad], self.output)
        self.assertFalse(report["complete"])
        self.assertEqual(report["selected_prs"], 1)
        result = subprocess.run([sys.executable, str(Path(aggregate.__file__)), str(valid), str(bad), "--output", str(self.output)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)

    def test_malformed_input_is_reported_and_valid_input_still_summarized(self):
        valid = self.save("valid", self.record())
        bad = self.root / "broken.json"
        bad.write_text("not JSON")
        report = aggregate.aggregate([valid, bad], self.output)
        self.assertFalse(report["complete"])
        self.assertEqual(report["selected_prs"], 1)
        self.assertEqual(len(report["errors"]), 1)

    def test_person_csv_guards_formulas_and_cli_success(self):
        path = self.save("formulas", self.record(name="=formula"))
        result = subprocess.run([sys.executable, str(Path(aggregate.__file__)), str(path), "--output", str(self.output)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("'=formula", (self.output / "people.csv").read_text(encoding="utf-8-sig"))
        for filename in ("summary.md", "summary.json", "people.csv", "pull-requests.csv"):
            self.assertTrue((self.output / filename).is_file())

    def test_person_csv_sums_rule_groups_without_repeating_person_totals(self):
        record = self.record()
        second = deepcopy(record["findings"][0])
        second["rule"] = "SecondRule"
        second["confidence"] = "low"
        record["findings"].append(second)
        aggregate.aggregate([self.save("two-rules", record)], self.output)
        with (self.output / "people.csv").open(encoding="utf-8-sig", newline="") as handle:
            rows = list(csv.DictReader(handle))
        self.assertEqual(sum(int(row["violation_count"]) for row in rows), 2)
        self.assertEqual({row["confidence"] for row in rows}, {"high", "low"})


if __name__ == "__main__":
    unittest.main()
