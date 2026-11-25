#!/usr/bin/env bash
# Usage: ./run_three_times.sh [chrome|edge|firefox] [true|false for headless]
# Runs the full suite 3 times in a row and keeps each run's TestNG report in run-reports/.
BROWSER="${1:-chrome}"
HEADLESS="${2:-false}"
for i in 1 2 3; do
  echo "=== Run $i of 3 on $BROWSER ==="
  mvn -q clean test -Dbrowser="$BROWSER" -Dheadless="$HEADLESS"
  mkdir -p "run-reports/${BROWSER}_run${i}"
  cp -r target/surefire-reports/. "run-reports/${BROWSER}_run${i}/"
  [ -d target/screenshots ] && cp -r target/screenshots "run-reports/${BROWSER}_run${i}/"
done
echo "Done. Open run-reports/${BROWSER}_run1/index.html (and run2, run3)."
