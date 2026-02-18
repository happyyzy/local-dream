# SDXL Showcase (Fork Main)

This page is for fork-main public showcase.

## Real Benchmarks Collected From Local/ADB

| Source | Date | Model | Resolution | Steps | Sampler | Median (ms) | P90 (ms) | Scope |
|---|---|---|---|---:|---|---:|---:|---|
| Local profiling artifact | 2026-01-26 | sdxl | 1024 | 20 | qnn_unet_profile | 3437.27 | 3437.27 | UNet graph-only (not e2e) |
| ADB phone log | 2026-02-15 | z_image_turbo_q4 | 1024 | 4 | euler | 212970.00 | 212970.00 | End-to-end generate_image |
| ADB phone log | 2026-02-18 | z_image_turbo_q4 | 1024 | 20 | euler | 32870.00 | 32870.00 | VAE decode stage only |

Raw evidence:
- `benchmarks/results/raw_logs/step13_full_run.log`
- `benchmarks/results/raw_logs/run_step20_qcomml_1024_fallback_v_phone.log`
- `/home/happyyzy/output/sdxl_b1_1024_prof_detail_20260126_1621/sdxl_profile.csv`

## Gallery

- `benchmarks/images/phone_step20_qcomml_1024_fallback_v.png`
- `benchmarks/images/phone_step20_ggml_1024_ref_v3.png`
- `benchmarks/images/local_sdxl_baseline_1024_cfg75_seed0.png`

Each image has a same-name metadata file (`.json`) with source path and run context.
