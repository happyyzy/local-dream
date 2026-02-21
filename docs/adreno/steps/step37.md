# Step37

## 目标
未下载用户在 UI 中能看到两模型并可点击下载（Hugging Face）。

## 结果
- HF manifest 可访问：
  - `https://huggingface.co/zhiyuanasad/flux2_klein_adreno/resolve/main/download_manifest.json`
  - `https://huggingface.co/zhiyuanasad/z_image_turbo_adreno/resolve/main/download_manifest.json`
- 设备端通过“临时移走模型目录”模拟未下载，点击模型后弹出下载确认框。

## 证据
- XML：
  - `exp_20260221_goal_restart_step31/step37_download_verify/flux2_after_tap.xml`
  - `exp_20260221_goal_restart_step31/step37_download_verify/zimg_after_tap.xml`
- 图像：
  - `docs/adreno/assets/step37/step37_flux_download_dialog.png`
  - `docs/adreno/assets/step37/step37_zimg_download_dialog.png`
