#!/usr/bin/env bash
set -euo pipefail

tracked_generated="$(git ls-files | grep -E '(^|/)(target|node_modules|\.next)(/|$)' || true)"
if [[ -n "$tracked_generated" ]]; then
  echo "Generated artifacts are tracked:"
  echo "$tracked_generated"
  exit 1
fi

secret_hits="$(git grep -nEI '(rzp_live_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{30,}|-----BEGIN ([A-Z ]+ )?PRIVATE KEY-----)' -- . ':!scripts/verify-no-secrets.sh' || true)"
if [[ -n "$secret_hits" ]]; then
  echo "Possible credential material found:"
  echo "$secret_hits"
  exit 1
fi

echo "Repository hygiene and credential-pattern scan passed."

