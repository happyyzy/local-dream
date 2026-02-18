#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate calibration manifest file.")
    parser.add_argument("--input-dir", required=True, type=Path, help="Directory with calibration images.")
    parser.add_argument("--output", required=True, type=Path, help="Output manifest path.")
    parser.add_argument(
        "--extensions",
        default=".png,.jpg,.jpeg,.webp",
        help="Comma-separated image extensions.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    input_dir: Path = args.input_dir
    output: Path = args.output
    extensions = {ext.strip().lower() for ext in args.extensions.split(",") if ext.strip()}

    if not input_dir.exists() or not input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {input_dir}")

    files = [
        p
        for p in input_dir.rglob("*")
        if p.is_file() and p.suffix.lower() in extensions
    ]
    files.sort()

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as f:
        for file_path in files:
            f.write(f"{file_path.as_posix()}\\n")

    print(f"Wrote {len(files)} entries to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
