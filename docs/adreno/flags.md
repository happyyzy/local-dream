# Adreno Flags

> 口径：local-dream 的 `sdcpp_backend.json` 与 profile 覆盖项。

| Flag | 默认/常用值 | 作用范围 | 性能/精度影响 | 是否临时 |
|---|---|---|---|---|
| `flash_attn` | 512: `false`, 1024: `true` | 主干 attention | 1024 不开会 OOM；512 开启通常更慢 | 否 |
| `threads` | `1` | sd.cpp 执行线程 | 当前稳定性与一致性优先 `1` | 否 |
| `mldrift` | Klein1024/ZI1024: `1` | 主干 attention 实现 | 显著提速，需配套数值验证 | 否 |
| `mldrift_io_first` | `1` | mldrift I/O 策略 | 影响吞吐与内存行为 | 否 |
| `mldrift_chunk_kv` | `64` | mldrift KV 分块 | 影响峰值内存/吞吐 | 否 |
| `qcom_ml_prepare` | 512: `1`, 1024 常用 `0/1` | QCOM ML VAE/桥接准备 | 影响首次开销与稳定性 | 否 |
| `qcom_ml_try_tiled` | 512: `0`, 1024 可 `1` | QCOM ML VAE 分块尝试 | 可能稳但通常更慢 | 否 |
| `qcom_ml_host_attn` | 512 edit: `0`, 1024 常 `1` | VAE 长序列 attention 回退 | 决定是否走 host attention | 否 |
| `qcom_ml_host_attn_backend` | `mldrift` / `ggml` | host attention 后端 | `mldrift` 通常更快 | 否 |
| `qcom_ml_fallback_attn_16384` | edit512: `1` | L=16384 不支持节点 | 保证可运行，可能略降速 | 否 |
| `qcom_ml_disable_mnn_attn` | `1` | 桥接层 attention 选择 | 强制避开 MNN attention | 否 |
| `qcom_ml_optimize_mem` | `1` | QCOM ML 内存策略 | 降显存峰值，可能影响吞吐 | 否 |
| `vae_backend` | `qcom_ml` | VAE 后端 | 512/1024 decode 加速关键 | 否 |
| `vae_conv_direct` | `true` | ggml/OpenCL VAE 卷积路径 | 对 VAE 延迟有明显影响 | 否 |
| `qcom_ml_replay_q_scale_mul` | 常用 `1.0` | replay attention 缩放 | 调整数值拟合，需对齐验证 | 临时/实验 |
| `extra_env.SD_OCL_Q4_GEMM_*` | 按模型 profile | Q4 GEMM 策略 | 可提速，需模型级回归 | 临时/实验 |
| `use_vae_tiling`（请求字段） | Step36 对 `sdxl_npu_1024` 强制 `false` | NPU SDXL 1024 decode | 直接把 20 步从 50s+ 拉回 30s 量级 | 是（仅 step36 兼容修补） |

主要实测配置样例：
- `exp_20260220_redo/step38_pr_acceptance/logs/flux_cfg_runtime_current.json`
- `exp_20260220_redo/step38_pr_acceptance/logs/zimg_cfg_runtime_current.json`
