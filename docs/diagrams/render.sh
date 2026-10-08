#!/usr/bin/env bash
# Run from any directory. Tool binaries stay outside the documentation sources.
set -euo pipefail
DIAGRAM_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
: "${PLANTUML_JAR:?Set PLANTUML_JAR to the local PlantUML 1.2026.8 JAR}"
JAVA_BIN="${JAVA_BIN:-java}"
export PLANTUML_LIMIT_SIZE="${PLANTUML_LIMIT_SIZE:-16384}"
if [[ ! -f "$PLANTUML_JAR" ]]; then
  printf 'PlantUML JAR not found: %s\n' "$PLANTUML_JAR" >&2
  exit 1
fi
case "${1:-render}" in
  check|render) MODE="${1:-render}" ;;
  *) printf 'Usage: bash render.sh [check|render]\n' >&2; exit 2 ;;
esac
shopt -s nullglob
DIAGRAMS=()
for SOURCE in "$DIAGRAM_DIR"/*.puml; do
  [[ "$(basename -- "$SOURCE")" == _* ]] && continue
  DIAGRAMS+=("$SOURCE")
done
if [[ ${#DIAGRAMS[@]} -eq 0 ]]; then
  printf 'No standalone PlantUML diagrams found\n' >&2
  exit 1
fi
# Syntax validation must succeed before any images are generated.
"$JAVA_BIN" -Djava.awt.headless=true -jar "$PLANTUML_JAR" --check-syntax "${DIAGRAMS[@]}"
if [[ "$MODE" == render ]]; then
  "$JAVA_BIN" -Djava.awt.headless=true -jar "$PLANTUML_JAR" --format svg --stop-on-error --no-error-image "${DIAGRAMS[@]}"
  for SOURCE in "${DIAGRAMS[@]}"; do
    [[ -s "${SOURCE%.puml}.svg" ]] || { printf 'Missing SVG: %s\n' "$SOURCE" >&2; exit 1; }
  done
fi
printf 'Verified %s diagrams (%s)\n' "${#DIAGRAMS[@]}" "$MODE"
