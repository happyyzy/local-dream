# Performance Methodology

## 目的
保证“SDXL 1024 极速”叙述可复现、可对比、可审查。

## 必填环境信息
- 设备型号
- 固件/系统版本（例如 `v79`）
- App 版本与提交哈希
- 模型版本与量化配置

## 运行条件
- 分辨率（如 512 / 1024）
- 步数（steps）
- 采样器（sampler）
- CFG
- warmup 次数

## 统计口径
- 主指标：`s/it`
- 补充指标：`median latency (ms)`、`p90 latency (ms)`
- 其中 `s/it = generationTime / steps`

## 输出格式
结果统一写入 `benchmarks/results/benchmark_results.csv`。
字段说明：
- `date_utc`
- `model`
- `resolution`
- `steps`
- `sampler`
- `samples`
- `median_ms`（可由 `s/it * 1000` 换算）
- `p90_ms`
- `notes`
