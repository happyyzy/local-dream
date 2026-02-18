#!/usr/bin/env bash
set -euo pipefail

required_files=(
  README.md
  CONTRIBUTING.md
  CHANGELOG.md
  CODEOWNERS
  docs/branching.md
  docs/fork_bootstrap.md
  docs/performance/methodology.md
  docs/compliance.md
  scripts/bootstrap_git_branches.sh
  scripts/convert_sd15.sh
  scripts/convert_sdxl_512.sh
  scripts/convert_sdxl_1024.sh
  scripts/prepare_calibration_manifest.py
  scripts/run_benchmark.py
  tools/convert_entry.py
  benchmarks/results/benchmark_results.csv
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

bash -n scripts/bootstrap_git_branches.sh
bash -n scripts/convert_sd15.sh
bash -n scripts/convert_sdxl_512.sh
bash -n scripts/convert_sdxl_1024.sh
bash -n tools/check_repo.sh

python3 - <<'PY'
from pathlib import Path

for rel in [
    "scripts/prepare_calibration_manifest.py",
    "scripts/run_benchmark.py",
    "tools/convert_entry.py",
]:
    src = Path(rel).read_text(encoding="utf-8")
    compile(src, rel, "exec")
PY

echo "Repository checks passed."
