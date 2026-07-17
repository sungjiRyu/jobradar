# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. Issue & Branch Workflow

**Create an issue and branch before implementing new features or bug fixes.**

For any new feature or bug fix:
- Create a GitHub issue before making code changes.
- Create and switch to a dedicated branch before making code changes.
- Use the existing project issue style and keep titles/bodies consistent.
- If the task is only planning, investigation, explanation, or UI mock review, an issue/branch is not required unless implementation begins.

Issue title format:
```
[feat] Short feature summary
[fix] Short bug summary
[chore] Short maintenance summary
```

Issue body format:
```
## 🛠️ 작업 내용
- [Concrete task 1]
- [Concrete task 2]

## 📌 변경 이유
- [Why this change is needed]

## ✅ 체크리스트
- [ ] [Verification or implementation step 1]
- [ ] [Verification or implementation step 2]
```

Branch naming:
```
feat/issue-{number}-{short-kebab-summary}
fix/issue-{number}-{short-kebab-summary}
chore/issue-{number}-{short-kebab-summary}
```

Example:
```
Issue: [feat] 인기 공고 랭킹 API 및 메인 위젯 연동
Branch: feat/issue-123-trending-jobs-ranking
```

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
