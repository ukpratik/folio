#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Fails if a release APK requests any permission outside the allowlist (ADR-0013, FR-30/31).
set -euo pipefail
ALLOWED=("android.permission.CAMERA")
# Added by AndroidX core to guard unexported receivers; it's a signature-level app-private permission, not a user one.
ALLOWED_SUFFIX=".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
AAPT2=$(ls -d "$SDK"/build-tools/*/aapt2 | sort -V | tail -1)
status=0
for apk in "$@"; do
  echo "Checking $apk"
  while read -r perm; do
    [[ -z "$perm" ]] && continue
    if [[ " ${ALLOWED[*]} " == *" $perm "* || "$perm" == *"$ALLOWED_SUFFIX" ]]; then
      echo "  ok    $perm"
    else
      echo "  BLOCK $perm"; status=1
    fi
  done < <("$AAPT2" dump permissions "$apk" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p")
done
exit $status
