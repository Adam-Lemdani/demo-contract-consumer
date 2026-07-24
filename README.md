# demo-contract-consumer

A minimal Spring Boot **consumer** that calls the provider's greetings endpoint
and verifies against its stubs using **Spring Cloud Contract Stub Runner** with
`StubsMode.LOCAL` - **no broker**, **no artifact publishing**.

- Java 21, Maven, Spring Boot 3.4.1, Spring Cloud 2024.0.0.
- Client: `GreetingClient` → `GET /api/greetings/{name}`.
- Integration test: `GreetingClientStubRunnerTest` replays the provider's LOCAL
  stubs.
- Provider stub coordinates are **overridable properties**, not hard-coded
  through the code. The **upstream dependency is declared once** in
  `src/test/resources/application.yml` (`stubrunner.ids`) — read by both Spring
  Cloud Contract Stub Runner and the CI discovery step. The version is a
  placeholder (`${provider.stubs.version:1.0.0-SNAPSHOT}`) forwarded from Maven
  into the test JVM by surefire, so CI can override it per build.

## What the demo proves / does not prove

**Proves:** A consumer PR (or an upstream provider PR) triggers an automated flow
that runs the **real consumer tests** against provider stubs built from an exact
commit, in a runner-local `.m2`, and reports pass/fail on the PR — no manual
Maven, nothing published.

**Does not prove:** Full discovery of all provider/consumer relationships, or
correctness of runtime concerns outside the contract. Building the provider and
"installing a jar" does not verify consumer behaviour; these workflows explicitly
run the changed consumer tests.

## Architecture / PR verification flow

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant CP as Consumer PR
    participant D as discover job
    participant M as verify matrix (parallel, 1 job / provider)
    participant R as report job
    participant GH as GitHub API

    Dev->>CP: Open / update PR
    CP->>D: cross-repo-verify.yml
    D->>D: read UPSTREAM coords from application.yml (stubrunner.ids)
    D->>GH: resolve + confirm owning provider repo(s)
    D-->>M: matrix = [confirmed providers]
    par one job per provider (fail-fast:false, max-parallel:10)
        M->>M: checkout provider@default (depth 1) + consumer@SHA (depth 1)
        M->>M: mvn install provider -> runner-local .m2 (stubs)
        M->>M: mvn test consumer@SHA (StubsMode.LOCAL)
        M->>R: upload result artifact (pass/fail + job link)
    end
    R->>GH: single commit status + one summary comment
    GH-->>CP: ✅/❌ required status check
```

> Each matrix job checks out only **two** repos with `fetch-depth: 1`; providers
> are usually one, so this is fast. See the root presentation guide for the full
> performance rationale.

## Local test instructions

The consumer needs the provider stubs in your local `.m2` first:

```bash
# 1) In the provider repo:
( cd ../demo-contract-provider && mvn clean install )

# 2) Then run the consumer tests (positive scenario):
mvn clean test

# Override the stub version to match a specific provider build:
mvn clean test -Dprovider.stubs.version=1.0.0-SNAPSHOT
```

### Positive and breaking scenarios

- **Positive** — `GreetingClientStubRunnerTest`: runs by default, asserts
  `Hello Adam` from the LOCAL stub.
- **Breaking** — `BreakingConsumerScenarioTest`: skipped by default; enable with:

  ```bash
  mvn test -Pbreaking-demo
  ```

  It calls an **uncontracted path** (`/api/salutations/{name}`) the stub does not
  serve, demonstrating that a consumer change to a different path / unsupported
  response is detected against the provider stubs.

A real breaking consumer change (e.g. editing `GreetingClient` to call the wrong
path or expect a missing field) makes `GreetingClientStubRunnerTest` fail — which
is exactly what cross-repo verification reports on the PR.

## Workflows

| File | Trigger | Purpose |
|------|---------|---------|
| `pr-build.yml` | `pull_request`, `workflow_dispatch` | Checkout provider default, install stubs to runner-local `.m2`, run consumer tests. |
| `cross-repo-verify.yml` | `pull_request`, `workflow_dispatch` | `discover` upstream providers from `application.yml` (`stubrunner.ids`) → `verify` matrix (parallel, one job per provider; builds provider stubs, runs this consumer's PR tests) → `report` single status + summary comment. |

`cross-repo-verify.yml` accepts a `workflow_dispatch` input (`provider_repo`) to
verify against a single provider manually.

> **Alternative (cross-org / partner-owned):** swap the matrix for a
> `repository_dispatch` to the provider repo plus a receiver workflow on its
> default branch (needs `Contents: write`). The in-repo matrix is used here for
> speed and self-containment.

## GitHub App / PAT setup

Same model as the provider. Production uses a **GitHub App** on both repos; a
fine-grained **PAT** is a demo-only fallback.

### Required GitHub App permissions (verify against current GitHub docs)

| Scope | Access | Why |
|-------|--------|-----|
| **Metadata** | Read | Always required. |
| **Contents** | Read (both repos) | Check out this repo + the provider (private) in the matrix. `Contents: write` only for the `repository_dispatch` alternative. |
| **Commit statuses** | Read + write | Single branch-protection status check. |
| **Pull requests** | Read + write | Single summary comment. |
| **Checks** | Write | Only if using Check Runs instead of commit statuses. |

> Verified: the `repository_dispatch` alternative requires `Contents: write`
> (https://docs.github.com/en/rest/repos/repos#create-a-repository-dispatch-event).
> The in-repo matrix needs only `Contents: read` on both repos + statuses/PR write.

### Repository variables and secrets

| Name | Kind | Notes |
|------|------|-------|
| `APP_ID` | Variable | GitHub App id. Empty → PAT fallback. |
| `PARTNER_REPO` | Variable | `owner/demo-contract-provider`. Also used by `pr-build.yml` to fetch stubs. |
| `APP_PRIVATE_KEY` | Secret | GitHub App private key (PEM). |
| `DISPATCH_PAT` | Secret | Fine-grained PAT, demo only. |

## Dependency discovery (and its limits)

Three tiers, in priority order:

1. **Service graph (Tier-3, preferred)** — `.github/service-graph.json`, read
   first by the `discover` job. `upstreamProviders` is regenerated nightly by
   `.github/workflows/service-graph-generator.yml` from `application.yml`
   (`stubrunner.ids`) resolved via confirmed Code Search;
   `manualOverrides.upstreamProviders` is preserved. Bare names get the repo
   owner inferred at runtime.
2. **`PARTNER_REPO`** — deterministic fallback.
3. **application.yml coordinates + Code Search** — the consumer's upstream is
   declared authoritatively in `stubrunner.ids`; Code Search only resolves which
   repo *produces* those coordinates, and is text-based / not authoritative.

Every candidate is **confirmed** by reading its `pom.xml` before verification.
The generator opens a PR rather than pushing to `main`, so changes are audited.

## Security model / limitations

- Fork PRs never receive credentials.
- Payloads validated (SHA/repo/PR) to block repository/command injection.
- Verification always uses the originating PR SHA, never a branch name.
- Least-privilege `permissions:`, `timeout-minutes`, and `concurrency` cancel
  superseded runs. App tokens scoped to the two repos.

## Troubleshooting

- **`StubNotFoundException`**: provider not installed into the job `.m2`, or
  `provider.stubs.version` mismatch.
- **Filtered property literal (`@...@`) in test config**: run through Maven
  (`mvn test`), not the IDE without resource filtering.
- **Discovery empty**: set `PARTNER_REPO` or use the `workflow_dispatch` inputs.
