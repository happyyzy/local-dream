#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   bash scripts/bootstrap_git_branches.sh <official_upstream_url>
# Example:
#   bash scripts/bootstrap_git_branches.sh git@github.com:official/local-dream.git

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <official_upstream_url>" >&2
  exit 1
fi

upstream_url="$1"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Current directory is not a git repo. Run: git init" >&2
  exit 1
fi

current_branch="$(git branch --show-current 2>/dev/null || true)"
if [[ -z "$current_branch" ]]; then
  echo "No current branch detected; create an initial commit first." >&2
  exit 1
fi

if git remote get-url upstream >/dev/null 2>&1; then
  git remote set-url upstream "$upstream_url"
else
  git remote add upstream "$upstream_url"
fi

git fetch upstream

if git show-ref --verify --quiet refs/heads/upstream-sync; then
  git checkout upstream-sync
else
  git checkout -b upstream-sync upstream/master
fi

if git show-ref --verify --quiet refs/heads/main; then
  git checkout main
else
  git checkout -b main upstream-sync
fi

echo "Bootstrap complete."
echo "Next: git push -u origin upstream-sync main"
