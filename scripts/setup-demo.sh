#!/usr/bin/env bash
# Usage:
#   export DISPATCH_PAT=github_pat_xxx
#   ./scripts/setup-demo.sh            # infers owner from `gh api user`
#   ./scripts/setup-demo.sh <owner>    # or pass your username explicitly
set -euo pipefail

SELF_REPO="demo-contract-consumer"
PARTNER_REPO_NAME="demo-contract-provider"
REQUIRED_CONTEXT="contract-verification"

OWNER="${1:-$(gh api user --jq .login)}"
echo "Configuring ${OWNER}/${SELF_REPO} (partner: ${OWNER}/${PARTNER_REPO_NAME})"

if [[ -z "${DISPATCH_PAT:-}" ]]; then
  echo "ERROR: export DISPATCH_PAT=<fine-grained-pat> first." >&2
  exit 1
fi

# 1) Deterministic partner for discovery + reporting target (also used by pr-build.yml).
gh variable set PARTNER_REPO -R "${OWNER}/${SELF_REPO}" --body "${OWNER}/${PARTNER_REPO_NAME}"

# 2) Demo credential the workflows fall back to when APP_ID is unset.
printf '%s' "${DISPATCH_PAT}" | gh secret set DISPATCH_PAT -R "${OWNER}/${SELF_REPO}"

# 3) Require the verification status check before merge (this is what BLOCKS merging).
gh api -X PUT "repos/${OWNER}/${SELF_REPO}/branches/main/protection" \
  -H "Accept: application/vnd.github+json" \
  -f "required_status_checks[strict]=true" \
  -f "required_status_checks[contexts][]=${REQUIRED_CONTEXT}" \
  -F "enforce_admins=true" \
  -f "required_pull_request_reviews[required_approving_review_count]=0" \
  -F "restrictions="

echo "Done. ${OWNER}/${SELF_REPO}: PARTNER_REPO + DISPATCH_PAT set, '${REQUIRED_CONTEXT}' required on main."
