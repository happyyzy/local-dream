#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <checkpoint> <output_dir> <calibration_manifest> [extra args...]" >&2
  exit 1
fi

checkpoint="$1"
output_dir="$2"
calib_manifest="$3"
shift 3

python3 tools/convert_entry.py \
  --model sdxl \
  --resolution 512 \
  --checkpoint "$checkpoint" \
  --output-dir "$output_dir" \
  --calibration-manifest "$calib_manifest" \
  "$@"
