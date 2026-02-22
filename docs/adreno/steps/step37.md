# Step37

## 目标
未下载用户在 UI 中能看到模型并可点击下载（Hugging Face）。

## 结果
- HF manifest 可访问：
  - `https://huggingface.co/zhiyuanasad/flux2_klein_adreno/resolve/main/download_manifest.json`
  - `https://huggingface.co/zhiyuanasad/z_image_turbo_adreno/resolve/main/download_manifest.json`
  - `https://huggingface.co/zhiyuanasad/sdxl_npu_1024_ctxbin/resolve/main/download_manifest.json`（dispatcher 命中）
  - `https://huggingface.co/zhiyuanasad/sdxl_npu_1024_unified/resolve/main/download_manifest.json`（fallback）
- 设备端通过“临时移走模型目录”模拟未下载，点击模型后弹出下载确认框。
- SDXL NPU 卡片标题/描述使用与现有 SD1.5 NPU 一致的样式（同一 Material typography）。
- UI 仅展示一个 `SDXL Base` 入口；下载时按 SoC dispatcher 选择 manifest（当前 Elite 类 SoC 走 ctxbin）。

## SDXL 两种包的区别（本轮新增）
- `sdxl_npu_1024_ctxbin`：
  - `unet_1024.bin` 使用当前验证链路的 context binary（dspArch=79，graph: `qnn_base_unet_w8a16_b1_1024_steps20_28_cfg7_p97_200_b1onnx`）。
- `sdxl_npu_1024_unified`：
  - 使用 `unet_1024_unified.bin`（legacy 兼容包，dspArch=79，graph: `qnn_base_unet_w8a16_b1_1024_comfy_cfg7_30samples_20260108_233355`），下载后映射到 `unet_1024.bin` 供现有后端直接使用。
- 二者共享其余 SDXL 资产（`unet.bin`/`vae_decoder*.bin`/`clip*.mnn`/`tokenizer*.json`/`npucustom`/`force_nhwc`）。

## 证据
- XML：
  - `exp_20260221_goal_restart_step31/step37_download_verify/flux2_after_tap.xml`
  - `exp_20260221_goal_restart_step31/step37_download_verify/zimg_after_tap.xml`
  - `docs/adreno/assets/step37/step37_sdxl_cards.xml`
  - `docs/adreno/assets/step37/step37_sdxl_ctx_tap.xml`
  - `docs/adreno/assets/step37/step37_sdxl_unified_tap.xml`
  - `docs/adreno/assets/step37/step37_sdxl_base_tap.xml`
- 图像：
  - `docs/adreno/assets/step37/step37_flux_download_dialog.png`
  - `docs/adreno/assets/step37/step37_zimg_download_dialog.png`
  - `docs/adreno/assets/step37/step37_sdxl_cards.png`
  - `docs/adreno/assets/step37/step37_sdxl_ctx_tap.png`
  - `docs/adreno/assets/step37/step37_sdxl_unified_tap.png`
  - `docs/adreno/assets/step37/step37_sdxl_base_tap.png`
