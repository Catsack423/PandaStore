#!/usr/bin/env bash
set -euo pipefail
report_dir="${PMD_REPORT_DIR:-code/backend/target/pmd-diff}"
: "${PMD_BIN:?Set PMD_BIN to the pinned PMD CLI executable}"
"$PMD_BIN" --version > "$report_dir/pmd-version.txt" 2>&1
java -version > "$report_dir/java-version.txt" 2>&1
overall=0
for format in json xml; do
  status=0
  args=(check --file-list "$report_dir/files.txt"
    --rulesets "$report_dir/ruleset.xml" --use-version java-21
    --minimum-priority 5 --format "$format" --report-file "$report_dir/pmd.$format"
    --relativize-paths-with "$PWD" --no-cache --no-progress
    --show-suppressed --benchmark --fail-on-violation --fail-on-error)
  printf '%q ' "$PMD_BIN" "${args[@]}" > "$report_dir/command-$format.txt"
  printf '\n' >> "$report_dir/command-$format.txt"
  "$PMD_BIN" "${args[@]}" > "$report_dir/pmd-$format.log" 2>&1 || status=$?
  printf '%s\n' "$status" > "$report_dir/exit-$format.txt"
  # Preserve native code 4 in the artifacts; finalize applies the approved
  # changed-line gate. Tool/parse/configuration errors still fail this step.
  if [[ "$status" != 0 && "$status" != 4 ]]; then overall=1; fi
done
# The finalize step records the result even when this step fails.
exit "$overall"
