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
- batch size
- warmup 次数

## 统计口径
- 主指标：`median latency (ms)`
- 辅指标：`p90 latency (ms)`
- 建议至少记录 5 次有效样本

## 输出格式
结果统一写入 `benchmarks/results/benchmark_results.csv`。
字段说明：
- `date_utc`
- `model`
- `resolution`
- `steps`
- `sampler`
- `samples`
- `median_ms`
- `p90_ms`
- `notes`
