# Calibration Assets

目录约定：
- `raw/`：原始校准图像（建议 LFS）
- `manifest.txt`：按行列出用于量化/校准的文件路径
- `manifest.example.txt`：样例清单

生成 manifest：
```bash
python3 scripts/prepare_calibration_manifest.py \
  --input-dir assets/calibration/raw \
  --output assets/calibration/manifest.txt
```
