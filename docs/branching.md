# Branching Strategy

## Branch Roles
- `upstream-sync`
  - 仅用于同步官方仓库变化。
  - 不接受本地功能开发提交。
- `main`
  - 可提交 PR 的稳定开发线。
  - 主页与文档默认展示这条线。
- `exp/*`
  - 调试、试验、一次性验证分支。
  - 不直接面向外部承诺稳定性。

## Merge Policy
1. `upstream-sync` -> `main`：只做同步与冲突解决。
2. `exp/*` -> `main`：必须先整理为可审查的最小变更。
3. `main` 要求 CI 通过，至少 1 次审查（建议）。

## Protected Rules (GitHub)
建议对 `main` 开启：
- Require pull request before merging
- Require status checks to pass
- Restrict force push
