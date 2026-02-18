# SDXL Showcase (Fork Main)

This page is for fork-main public showcase, using only `io.github.xororz.localdream` artifacts.

## SDXL 1024 Real Device Speed (`s/it`)

Device baseline:
- Model: `2410DPN6CC`
- SoC: `SM8750`
- System: `HyperOS 3.0.7.0`

| Sample ID | Resolution | Steps | CFG | Scheduler | Generation Time | Speed (s/it) |
|---|---|---:|---:|---|---:|---:|
| `1769073919488` | 1024x1024 | 20 | 7 | euler | 32.8s | 1.64 |
| `1769073990211` | 1024x1024 | 20 | 7 | euler | 32.8s | 1.64 |
| `1769163563122` | 1024x1024 | 20 | 7 | euler | 34.8s | 1.74 |

Notes:
- Milestone memory baseline is around `~1.4 s/it (CFG on)`.
- Recent saved localdream history samples shown above are `~1.64-1.74 s/it`.

## Gallery

![sample-1769073919488](../../benchmarks/images/localdream_sdxl_1769073919488.png)
![sample-1769073990211](../../benchmarks/images/localdream_sdxl_1769073990211.png)
![sample-1769163563122](../../benchmarks/images/localdream_sdxl_1769163563122.png)

## Raw Metadata

- `benchmarks/images/localdream_sdxl_1769073919488.json`
- `benchmarks/images/localdream_sdxl_1769073990211.json`
- `benchmarks/images/localdream_sdxl_1769163563122.json`
