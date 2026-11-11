#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
splash="$root_dir/app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StartupSplashScreen.kt"

for theme in "$root_dir/app/src/main/res/values/themes.xml" "$root_dir/app/src/main/res/values-night/themes.xml"; do
  grep -q 'android:windowBackground">#FAF7F0' "$theme" || {
    echo "FAIL: old launch window background in $theme"
    exit 1
  }
done

grep -q 'Color(0xFF6B381A)' "$splash" || { echo 'FAIL: startup brown foreground missing'; exit 1; }
grep -q 'Color(0xFFFAF7F0)' "$splash" || { echo 'FAIL: startup cream background missing'; exit 1; }
if grep -Eq '0xFF3933A8|0xFF24213F|0xFFF8F7FF|0xFFEAF3FF|0xFFF7F7FF|0xFF626B98' "$splash"; then
  echo 'FAIL: retired violet/blue colours remain in startup splash'
  exit 1
fi

echo 'PASS: startup window and splash palette contract'
