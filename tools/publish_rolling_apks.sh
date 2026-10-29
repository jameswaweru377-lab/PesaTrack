#!/usr/bin/env bash
# Publish one verified, release-signed Android APK under exactly two stable names.
set -euo pipefail

: "${APK_PATH:?Signed release APK path is required}"
: "${ANDROID_HOME:?Android SDK is required}"
: "${GH_TOKEN:?GitHub publication token is required}"
: "${GH_REPO:?GitHub repository is required}"
: "${GITHUB_SHA:?Source commit is required}"
: "${PESATRACK_VERSION_CODE:?Android versionCode is required}"
: "${PESATRACK_VERSION_NAME:?Android versionName is required}"

if [[ ! "$PESATRACK_VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then
  echo 'Invalid PESATRACK_VERSION_CODE' >&2
  exit 1
fi

if [[ ! "$GITHUB_SHA" =~ ^[0-9a-f]{40}$ ]]; then
  echo 'Invalid GITHUB_SHA' >&2
  exit 1
fi

if [[ ! -s "$APK_PATH" ]]; then
  echo 'Expected signed APK is missing' >&2
  exit 1
fi

sdk_tools="$ANDROID_HOME/build-tools/36.0.0"
apksigner="$sdk_tools/apksigner"
aapt="$sdk_tools/aapt"
zipalign="$sdk_tools/zipalign"

verify_apk() {
  local file="$1" result metadata line cert version
  unzip -t "$file" >/dev/null || return 1
  "$zipalign" -c -v 4 "$file" >/dev/null || return 1
  result="$("$apksigner" verify --verbose --print-certs "$file")" || return 1
  metadata="$("$aapt" dump badging "$file")" || return 1

  line="$(printf '%s\n' "$metadata" | grep '^package:')" || return 1
  if [[ "$line" != *"name='com.kedevjimmy.pesatrack'"* ]]; then
    echo 'APK package name is not the approved PesaTrack application ID' >&2
    return 1
  fi
  version="$(printf '%s\n' "$line" | sed -n "s/.*versionCode='\([0-9][0-9]*\)'.*/\1/p")"
  cert="$(printf '%s\n' "$result" | sed -n 's/^Signer #[0-9]* certificate SHA-256 digest: //p')"
  if [[ -z "$version" || -z "$cert" || "$cert" == *$'\n'* ]]; then
    echo 'APK versionCode or single signing certificate cannot be verified' >&2
    return 1
  fi
  printf '%s\t%s\n' "$version" "$cert"
}

new_identity="$(verify_apk "$APK_PATH")"
new_version="${new_identity%%$'\t'*}"
new_cert="${new_identity#*$'\t'}"
if [[ "$new_version" != "$PESATRACK_VERSION_CODE" ]]; then
  echo 'APK versionCode differs from requested release version' >&2
  exit 1
fi

badging="$("$aapt" dump badging "$APK_PATH")"
for required in "sdkVersion:'24'" "targetSdkVersion:'36'" "native-code: 'arm64-v8a'"; do
  if [[ "$badging" != *"$required"* ]]; then
    echo 'Signed APK is missing required Android compatibility metadata' >&2
    exit 1
  fi
done

tmp_dir="$(mktemp -d)"
trap 'rm -r -- "$tmp_dir"' EXIT

if gh release view ci-apk >/dev/null 2>&1; then
  existing_assets="$(gh release view ci-apk --json assets --jq '.assets[].name')"
  if printf '%s\n' "$existing_assets" | grep -Fxq 'PesaTrack-Update.apk'; then
    gh release download ci-apk --pattern 'PesaTrack-Update.apk' --dir "$tmp_dir" >/dev/null
    old_identity="$(verify_apk "$tmp_dir/PesaTrack-Update.apk")"
    old_version="${old_identity%%$'\t'*}"
    old_cert="${old_identity#*$'\t'}"
    if [[ "$old_cert" != "$new_cert" ]]; then
      echo 'Rolling update signing certificate changed; refusing an incompatible update' >&2
      exit 1
    fi
    if (( 10#$new_version <= 10#$old_version )); then
      echo 'Rolling update versionCode must increase; refusing downgrade or repeat' >&2
      exit 1
    fi
  fi
else
  gh release create ci-apk --target "$GITHUB_SHA" \
    --title 'PesaTrack Rolling CI APK' --notes 'Signed update channel; see current release notes.' --prerelease
fi

cp -- "$APK_PATH" "$tmp_dir/PesaTrack-Full.apk"
cp -- "$APK_PATH" "$tmp_dir/PesaTrack-Update.apk"
cmp -s -- "$tmp_dir/PesaTrack-Full.apk" "$tmp_dir/PesaTrack-Update.apk"

gh release upload ci-apk \
  "$tmp_dir/PesaTrack-Full.apk" "$tmp_dir/PesaTrack-Update.apk" --clobber

# Remove old per-commit and debug attachments only after both signed uploads succeed.
assets_after_upload="$(gh release view ci-apk --json assets --jq '.assets[].name')"
while IFS= read -r asset; do
  case "$asset" in
    ''|PesaTrack-Full.apk|PesaTrack-Update.apk) ;;
    *) gh release delete-asset ci-apk "$asset" --yes ;;
  esac
done <<< "$assets_after_upload"

final_assets="$(gh release view ci-apk --json assets --jq '.assets[].name' | LC_ALL=C sort)"
if [[ "$final_assets" != $'PesaTrack-Full.apk\nPesaTrack-Update.apk' ]]; then
  echo 'Rolling release does not contain exactly Full and Update APKs' >&2
  exit 1
fi

gh release edit ci-apk \
  --title 'PesaTrack Rolling CI APK' \
  --notes "Release-signed PesaTrack from source commit ${GITHUB_SHA} (version ${PESATRACK_VERSION_NAME}, Android versionCode ${new_version}). Full and Update APKs contain the same verified binary. Update installs in place only when the phone's existing PesaTrack certificate is compatible; prior debug installations may require a separately approved, backed-up one-time migration. CI checks do not prove physical-phone upgrade or data retention. The auto-generated source archive belongs to the original ci-apk tag; use the stated source commit for this APK."

echo "Published exactly two rolling signed APK assets for versionCode ${new_version}"
sha256sum "$tmp_dir/PesaTrack-Full.apk"
