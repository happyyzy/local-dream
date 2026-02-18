#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Record model conversion request for reproducible workflow.")
    parser.add_argument("--model", required=True, choices=["sd15", "sdxl"])
    parser.add_argument("--resolution", required=True, type=int, choices=[512, 1024])
    parser.add_argument("--checkpoint", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--calibration-manifest", required=True, type=Path)
    parser.add_argument("--quant", default="w8a8")
    parser.add_argument("--target", default="qnn-npu")
    parser.add_argument("--extra", action="append", default=[])
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if not args.checkpoint.exists():
        raise SystemExit(f"Checkpoint not found: {args.checkpoint}")
    if not args.calibration_manifest.exists():
        raise SystemExit(f"Calibration manifest not found: {args.calibration_manifest}")

    args.output_dir.mkdir(parents=True, exist_ok=True)

    request = {
        "model": args.model,
        "resolution": args.resolution,
        "checkpoint": args.checkpoint.as_posix(),
        "output_dir": args.output_dir.as_posix(),
        "calibration_manifest": args.calibration_manifest.as_posix(),
        "quant": args.quant,
        "target": args.target,
        "extra": args.extra,
    }

    output_file = args.output_dir / "conversion_request.json"
    with output_file.open("w", encoding="utf-8") as f:
        json.dump(request, f, ensure_ascii=True, indent=2)

    print(f"Conversion request saved: {output_file}")
    print("Next: replace tools/convert_entry.py with your real conversion pipeline entry.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
