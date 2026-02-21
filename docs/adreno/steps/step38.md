# Step38

## 目标
按工程规范整理 `upstream/work/pr/debug` 分支，并对 Step31-36 做复杂人像全量验收。

## 分支与快照
- `work/main`：`bbaa04e`
- `debug/step38-pr-acceptance`：`bbaa04e`
- `pr/main`：`0a170fa`
- tags：`localdream-step36-src` / `localdream-step37-src` / `localdream-step38-src`
- milestones：`milestone/step36-source` / `milestone/step37-source` / `milestone/step38-source`

## 验收结果（关键口径）
- Step31（Klein512 4步）：`38.5s`
  - `exp_20260220_redo/step38_pr_acceptance/runs/step31_flux512_portrait_v24_noncat/run_001/1771681160201.json`
- Step32（Klein1024 4步）：`2m14s`（<=142s）
  - `exp_20260220_redo/step38_pr_acceptance/runs/step32_flux1024_portrait_v21_noncat_precond/run_001/1771682277780.json`
- Step33（ZI512 8步）：`1m7s`
  - `exp_20260220_redo/step38_pr_acceptance/runs/step33_zimg512_portrait_v27_noncat/run_001/1771682479860.json`
- Step34（ZI1024 8步）：`7m16s`
  - `exp_20260220_redo/step38_pr_acceptance/runs/step34_zimg1024_portrait_v6_recheck/run_001/1771672700835.json`
- Step35（edit ref2）：`1m9s`
  - `exp_20260220_redo/step38_pr_acceptance/runs/step35_edit_portrait_ref12_v14_editprecond/run_ref2/1771674862643.json`
- Step36（SDXL NPU 1024 20步）：`33.3s`
  - `exp_20260221_goal_restart_step31/step36_sdxl1024_20step_notile_portrait/1771708724515.json`

完整报告：
- `exp_20260220_redo/step38_pr_acceptance/report_step38_final_20260222.md`

图像示例：
- `docs/adreno/assets/step38/step38_step31_flux512.png`
- `docs/adreno/assets/step38/step38_step35_edit_ref2.png`
