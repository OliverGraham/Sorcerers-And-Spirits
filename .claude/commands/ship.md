---
description: Branch, commit, push, PR, merge, pull main, delete branch
---

Ship the current working-tree changes end-to-end, autonomously, without pausing for confirmation between steps:

1. `git checkout -b <descriptive-branch>` off `main`.
2. `git add` the relevant files + `git commit` (end message with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`).
3. `git push -u origin <branch>`.
4. `gh pr create` (end body with the Claude Code generated-with footer).
5. `gh pr merge <#> --merge` (merge commit, to match existing PR history).
6. `git checkout main && git pull origin main`.
7. `git branch -d <branch>`.

If `$ARGUMENTS` is given, use it as the commit/PR subject; otherwise derive one from the diff.
