#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture="$(jq -n '{appsec: {
  security_score: 64,
  high: [{title:"Exported component needs permission", section:"manifest", description:"private bank data and account 123456789"}],
  warning: [{title:"Weak certificate configuration", section:"certificate", description:"private path"}, {title:"<script>not safe</script>", section:"manifest"}],
  hotspot: [{title:"Review cryptographic implementation", section:"code"}]
}}')"

summary="$(printf '%s\n' "$fixture" | bash "$repo_root/tools/mobsf-safe-summary.sh" --titles)"
[[ "$summary" == *"Security score: 64/100"* ]]
[[ "$summary" == *"High findings: 1"* ]]
[[ "$summary" == *"Warnings: 2"* ]]
[[ "$summary" == *"Hotspots: 1"* ]]
[[ "$summary" == *"Exported component needs permission"* ]]
[[ "$summary" == *"Weak certificate configuration"* ]]
[[ "$summary" == *"[title withheld: unsafe characters]"* ]]
[[ "$summary" != *"private bank data"* ]]
[[ "$summary" != *"private path"* ]]
[[ "$summary" != *"<script>"* ]]

counts_only="$(printf '%s\n' "$fixture" | bash "$repo_root/tools/mobsf-safe-summary.sh")"
[[ "$counts_only" != *"Exported component"* ]]
[[ "$counts_only" == *"High findings: 1"* ]]

echo 'MobSF sanitized summary contract: PASS'
