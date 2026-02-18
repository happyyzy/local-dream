# Fork Bootstrap Guide

## 目标
把官方 `local-dream` fork 到你自己的仓库，并保持三条清晰线路：
- 官方同步线：`upstream-sync`
- 可提交 PR 线：`main`
- 调试线：`exp/*`

## 一次性初始化
```bash
git clone https://github.com/happyyzy/local-dream.git
cd local-dream
git remote add upstream https://github.com/xororz/local-dream.git
```

## 创建三条线
```bash
git fetch upstream
git checkout -b upstream-sync upstream/master
git checkout -b main upstream-sync
git push -u origin upstream-sync main
```

## 日常同步
```bash
git checkout upstream-sync
git fetch upstream
git merge --ff-only upstream/master

git checkout main
git merge upstream-sync
```

## 调试分支
```bash
git checkout -b exp/sdxl-1024-tuning main
# ...调试...
# 整理后把最小可审查提交合并回 main
```
