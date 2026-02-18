# Contributing Guide

## 目标
所有提交必须满足：
- 可复现：命令、参数、输入来源明确。
- 可审查：改动范围清晰，附带结果。
- 可回滚：不破坏 `main` 的 PR 质量。

## 分支与合并
1. 从 `main` 拉出功能分支：`feat/*`、`fix/*`、`docs/*`。
2. 调试实验统一放 `exp/*`，成熟后再挑选提交到 `main`。
3. `upstream-sync` 仅用于同步上游，禁止混入本地实验提交。

## PR 要求
1. 说明变更动机与影响范围。
2. 给出复现命令和关键日志/结果。
3. 若涉及性能宣称，必须同步更新 `benchmarks/results/benchmark_results.csv`。
4. 若新增大资产，说明存储位置（LFS/Release）与下载方法。

## 提交建议
推荐使用 Conventional Commits：
- `feat:` 新功能
- `fix:` 问题修复
- `docs:` 文档更新
- `perf:` 性能优化
- `chore:` 杂项维护
