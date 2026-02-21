# Step36

## 目标
确保接入 Adreno 后不破坏原有 SDXL NPU 1024 路由，并恢复到约 30s 量级（20 步）。

## 修补
- `app/src/main/java/io/github/xororz/localdream/service/BackgroundGenerationService.kt`
  - 对 `sdxl_npu_1024` 且 `1024x1024` 注入 `use_vae_tiling=false`。
- `app/src/main/cpp/src/main.cpp`
  - 保留 direct-1024 路由判定（`unet_1024.bin` + `vae_decoder_1024.bin`）。

## Before/After
- Before（同口径）：`53.9s`
  - `exp_20260221_goal_restart_step31/step36_sdxl1024_20step_recheck/1771707095123.json`
- After（复杂人像）：`33.3s`
  - `exp_20260221_goal_restart_step31/step36_sdxl1024_20step_notile_portrait/1771708724515.json`

## 路由证据
- `exp_20260221_goal_restart_step31/step36_sdxl1024_20step_notile_portrait/logcat_filtered.txt`
  - 命中 `unet_1024.bin` / `vae_decoder_1024.bin`
  - 日志出现 `VAE tiling disabled by request for 1024x1024 output.`

## 图像证据
- `docs/adreno/assets/step36/step36_sdxl1024_20step_33_3s.png`
