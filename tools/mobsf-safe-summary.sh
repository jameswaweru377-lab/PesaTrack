#!/usr/bin/env bash
# Accept MobSF JSON on stdin. Print only aggregate scores and opt-in safe rule
# titles; never emit descriptions, code, paths, identifiers, or raw reports.
set -euo pipefail

show_titles=false
if [[ "${1:-}" == '--titles' && "$#" == 1 ]]; then
  show_titles=true
elif [[ "$#" != 0 ]]; then
  echo 'Usage: mobsf-safe-summary.sh [--titles] < MobSF-report.json' >&2
  exit 2
fi

jq -er --argjson titles "$show_titles" '
  def safe_title:
    (.title // "" | tostring) as $value |
    if (($value | test("^[A-Za-z0-9 ,.:;()/_+\u0027-]{1,140}$")) and
        ($value | test("https?://|[0-9]{9,}"; "i") | not))
    then $value
    else "[title withheld: unsafe characters]"
    end;

  .appsec as $app |
  if (($app.security_score | type) != "number" or
      ($app.high | type) != "array" or
      ($app.warning | type) != "array" or
      ($app.hotspot | type) != "array")
  then error("MobSF result is missing expected scorecard fields")
  else
    "### MobSF static scan — aggregated indicators",
    "- Security score: \($app.security_score)/100",
    "- High findings: \($app.high | length)",
    "- Warnings: \($app.warning | length)",
    "- Hotspots: \($app.hotspot | length)",
    (if $titles then
      "### Rule titles (sanitized; no source details)",
      ($app.high[] | "- High: \(safe_title)"),
      ($app.warning[] | "- Warning: \(safe_title)"),
      ($app.hotspot[] | "- Hotspot: \(safe_title)")
     else empty end),
    "These are unverified scanner findings, not confirmed exploits."
  end
'
