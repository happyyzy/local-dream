#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import datetime as dt
from pathlib import Path
from statistics import median


def percentile(values: list[float], p: float) -> float:
    if not values:
        raise ValueError("empty values")
    if len(values) == 1:
        return values[0]
    sorted_values = sorted(values)
    k = (len(sorted_values) - 1) * p
    f = int(k)
    c = min(f + 1, len(sorted_values) - 1)
    if f == c:
        return sorted_values[f]
    d0 = sorted_values[f] * (c - k)
    d1 = sorted_values[c] * (k - f)
    return d0 + d1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Append benchmark metrics to CSV.")
    parser.add_argument("--model", required=True, choices=["sd15", "sdxl"])
    parser.add_argument("--resolution", required=True, type=int, choices=[512, 1024])
    parser.add_argument("--steps", required=True, type=int)
    parser.add_argument("--sampler", required=True)
    parser.add_argument(
        "--latencies-ms",
        required=True,
        help="Comma-separated latency list, e.g. 812,801,798",
    )
    parser.add_argument("--notes", default="")
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    latencies = [float(x.strip()) for x in args.latencies_ms.split(",") if x.strip()]
    if not latencies:
        raise SystemExit("latencies list is empty")

    row = {
        "date_utc": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d"),
        "model": args.model,
        "resolution": str(args.resolution),
        "steps": str(args.steps),
        "sampler": args.sampler,
        "samples": str(len(latencies)),
        "median_ms": f"{median(latencies):.2f}",
        "p90_ms": f"{percentile(latencies, 0.9):.2f}",
        "notes": args.notes,
    }

    output: Path = args.output
    output.parent.mkdir(parents=True, exist_ok=True)

    fieldnames = [
        "date_utc",
        "model",
        "resolution",
        "steps",
        "sampler",
        "samples",
        "median_ms",
        "p90_ms",
        "notes",
    ]

    write_header = not output.exists() or output.stat().st_size == 0
    with output.open("a", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        if write_header:
            writer.writeheader()
        writer.writerow(row)

    print(f"Appended benchmark row to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
