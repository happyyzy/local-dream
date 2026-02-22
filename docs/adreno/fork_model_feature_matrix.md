# Fork 模型与功能整理（Adreno / NPU）

> 目的：把当前 `work/main` 相对官方主线新增的模型、路由、验证入口集中到一页，降低后续维护混乱。

## 1) 新增模型入口（相对官方主线）

| UI 名称 | model id | 主要后端 | 分辨率显示 |
|---|---|---|---|
| Flux2 Klein Q4_0（Adreno优化） | `flux2_klein_adreno` | sd.cpp OpenCL（Adreno） | `512~1024` |
| Z Image Turbo Q4_0（Adreno优化） | `z_image_turbo_adreno` | sd.cpp OpenCL（Adreno） | `512~1024` |
| SDXL Base NPU | `sdxl_base_npu` | QNN/HTP | `1024×1024` |

代码入口：
- `app/src/main/java/io/github/xororz/localdream/data/Model.kt`
- `app/src/main/java/io/github/xororz/localdream/ui/screens/ModelListScreen.kt`

## 2) 下载与分发路由

### 2.1 Adreno（Flux2 / Z-Image）
- 通过 manifest 下载（含 `finished` 标记）：
  - `zhiyuanasad/flux2_klein_adreno`
  - `zhiyuanasad/z_image_turbo_adreno`

### 2.2 SDXL Base NPU（单卡入口 + SoC 分发）
- UI 仅展示单卡 `SDXL Base`，下载时按 SoC 分发：
  - Elite 类 SoC -> `sdxl_npu_1024_ctxbin`
  - 其他 -> `sdxl_npu_1024_unified`
- 代码：
  - `app/src/main/java/io/github/xororz/localdream/data/Model.kt`

## 3) 推理关键路由（运行时）

### 3.1 NPU 分辨率切换
- `1024x1024` 且存在 `unet_1024.bin` 时，走 direct-1024 二进制（跳过 patch）
- 其他非 512 分辨率才尝试 `*.patch`
- 代码：
  - `app/src/main/java/io/github/xororz/localdream/service/BackendService.kt`
  - `app/src/main/cpp/src/main.cpp`

### 3.2 SDXL 1024 的 decode 策略
- 对 SDXL NPU 1024 路由强制 `use_vae_tiling=false`（避免慢路径）
- 代码：
  - `app/src/main/java/io/github/xororz/localdream/service/BackgroundGenerationService.kt`

### 3.3 App 稳定性修补（服务重启/竞态）
- 后端启动与停止加同步，先停旧进程再拉新进程
- 防止 edit 双参考图的临时文件残留污染后续 txt2img
- 代码：
  - `app/src/main/java/io/github/xororz/localdream/service/BackendService.kt`
  - `app/src/main/java/io/github/xororz/localdream/ui/screens/ModelRunScreen.kt`

## 4) 验收文档与证据索引

- Step36（SDXL NPU 1024 性能与路由）：`docs/adreno/steps/step36.md`
- Step37（下载入口/单卡分发）：`docs/adreno/steps/step37.md`
- Step38（PR 验收总览）：`docs/adreno/steps/step38.md`
- 开关总表：`docs/adreno/flags.md`

UI 证据图集中在：
- `docs/adreno/assets/step36/`
- `docs/adreno/assets/step37/`
- `docs/adreno/assets/step38/`

## 5) 后续维护规则（建议执行）

1. 新增模型必须在本文件补齐：`model id`、manifest、分辨率显示、后端路由。
2. 每次改路由（BackendService / BackgroundGenerationService / ModelRunScreen）必须补 `docs/adreno/steps/stepXX.md`。
3. 验收口径固定：至少保留 1 组 `json + logcat + 输出图` 三件套。
4. 提交前检查 UI 文案与真实路由一致（避免“UI 可选但后端不支持”）。
