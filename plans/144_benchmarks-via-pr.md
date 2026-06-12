# Plan: Publish benchmark results via PR instead of direct push to master (#144)

**Branch**: features/144_benchmarks-via-pr
**Status**: Active
**Issue**: https://github.com/jeyben/fixedformat4j/issues/144
**Pattern source**: PR #143 (same fix applied to `pit-report.yml` for #142)

## Goal

The `Benchmarks` workflow publishes `docs/assets/benchmarks/` through a mergeable pull request instead of a direct push that protected `master` rejects.

## Background

`.github/workflows/benchmarks.yml` checks out `master`, regenerates `docs/assets/benchmarks/` (JMH results + meta per version, plus `index.json`), commits, and runs `git push` against `master`. Branch protection requires the `build` status check, so the push is rejected (`push declined due to repository rule violations`) — identical root cause to #142. The follow-up explicit `pages.yml` dispatch is then pointless.

Verified repo facts (gathered while fixing #142):

- Branch protection on `master`: required status check `build` (strict), no required reviews.
- The `build` check is produced by `nightly-build.yml`, which has a `workflow_dispatch` trigger.
- Pushes/PRs created with the default `GITHUB_TOKEN` do **not** trigger other workflows — the `build` check must be dispatched explicitly on the report branch or the PR can never become mergeable.
- "Allow GitHub Actions to create and approve pull requests" is enabled, so `gh pr create` works with the default token (needs `pull-requests: write`).
- Repo-level auto-merge is disabled — the results PR is merged manually once `build` is green.
- `pages.yml` deploys on pushes to `master` touching `docs/**`; a user-performed merge of the results PR triggers it, making the explicit dispatch step redundant.

## Scope note on TDD

This change is GitHub Actions YAML only — no production Java/TypeScript code, and the repository has no executable test harness for workflow definitions. The RED-GREEN-MUTATE cycle is therefore mapped to the closest observable equivalents: the failing behavior is the rejected push documented in #142/#144 (our standing RED), each step's GREEN is the workflow edit, and verification is YAML/actionlint validation locally plus a dispatched end-to-end run after merge. Mutation testing does not apply (and per project convention, PIT runs only in the GitHub Actions pipeline anyway). No changelog entry: CI-only change, not visible to library consumers.

## Acceptance Criteria

- [ ] Dispatching the `Benchmarks` workflow never attempts a direct push to `master`.
- [ ] A run with changed results opens a PR from a `chore/benchmarks-<run-id>` branch containing only `docs/assets/benchmarks/` changes, with the benchmarked versions named in the PR title/body.
- [ ] The required `build` check runs on that PR (via explicit `nightly-build.yml` dispatch on the branch), so the PR is mergeable once green.
- [ ] A run producing no staged changes exits successfully without creating a branch or PR. (JMH timing data differs run-to-run, so this path is rare — it fires only if the benchmark step produced nothing new — but it must not fail the job.)
- [ ] The workflow no longer dispatches `pages.yml`; the docs deployment happens via the merge to `master` (`docs/**` path filter on `pages.yml`).
- [ ] Workflow permissions are `contents: write`, `pull-requests: write`, `actions: write` — no broader.

## Steps

### Step 1: Replace the direct push with branch + PR + build dispatch

**RED (observed failing behavior)**: The current "Commit results" step's `git push` to `master` is rejected by branch protection — documented in #144; same failure mode as run 27411980154 for #142.
**GREEN (minimum change)**: Edit `.github/workflows/benchmarks.yml` only:

1. Add `pull-requests: write` to the `permissions` block (keep `contents: write` and `actions: write`).
2. Replace the "Commit results" step with an "Open PR with benchmark results" step that:
   - stages `docs/assets/benchmarks/` (pathspec staging covers deletions from the "Clean benchmark data" step on Git ≥ 2.0);
   - early-exits with a log message when `git diff --staged --quiet` reports no changes;
   - otherwise creates branch `chore/benchmarks-${{ github.run_id }}`, commits `bench: results for <versions>`, pushes the branch;
   - opens a PR against `master` via `gh pr create` (`GH_TOKEN: ${{ github.token }}`), body naming the benchmarked versions and explaining that merge triggers the pages deployment;
   - dispatches the required check: `gh workflow run nightly-build.yml --ref "$BRANCH"` — with a comment explaining why (GITHUB_TOKEN pushes don't trigger workflows).
3. Delete the "Trigger pages deployment" step.
4. Add a comment above the new step referencing #144 and the protected-master rationale, mirroring the #143 comment style.

**MUTATE / KILL MUTANTS**: Not applicable (YAML; see scope note).
**REFACTOR**: Compare side-by-side with `pit-report.yml` from PR #143 — keep step naming, comment style, and shell structure consistent between the two workflows so the next reader sees one pattern, not two variants.
**Done when**: YAML validates (`actionlint` if available, else a YAML parse), the diff touches only `benchmarks.yml`, and the step content matches the acceptance criteria above.

### Step 2: PR for the workflow change

Open a PR (`features/144_benchmarks-via-pr` → `master`) titled `ci: publish benchmark results via PR instead of direct push to master`, body referencing #144 and PR #143 as the established pattern, including the breaking-change checklist assessment (none of the framework-breaking surfaces in CLAUDE.md are touched — CI only). Delete this plan file in the same PR once implementation lands (plans are removed when complete).

**Done when**: PR is open, `build` is green on it, and it is merged.

### Step 3: Post-merge end-to-end verification

`workflow_dispatch` always runs the workflow file from `master`, so the new flow can only be exercised after the PR merges.

1. Dispatch `Benchmarks` with a small input (e.g. `1.8.0 master`) to keep the run short.
2. Verify: no push-to-master attempt; a `chore/benchmarks-<run-id>` PR exists; `nightly-build.yml` is running on its branch; the PR becomes mergeable when `build` passes.
3. Merge the results PR and confirm `pages.yml` deploys (triggered by the `docs/**` push to master).
4. Comment the outcome on #144 and close it.

**Done when**: #144 is closed with a link to a successful end-to-end run.

## Pre-PR Quality Gate

1. Mutation testing — not applicable (YAML only; PIT stays in the pipeline per project convention).
2. Refactoring assessment — consistency check against `pit-report.yml` (#143) done in Step 1.
3. Typecheck and lint — YAML validation (`actionlint` / parse check); no Java compiled.
4. DDD glossary — not applicable.

## Risks / open points

- **Sequencing with PR #143**: this plan assumes #143's pattern is accepted. If review of #143 changes the pattern (e.g. different branch naming or check-dispatch mechanism), mirror the final merged version here before implementing Step 1.
- **Benchmark run cost**: a full default run (`1.5.0 1.6.1 1.7.2 1.8.0 master`) takes long; Step 3 deliberately uses a reduced version list for verification.
- **Stale `chore/benchmarks-*` branches**: repo has `delete_branch_on_merge` disabled; merged report branches linger. Out of scope here — delete manually or enable the repo setting (note: also affects #143's `chore/pit-report-*` branches).

---
*Delete this file when the plan is complete. If `plans/` is empty, delete the directory.*
