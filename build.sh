#!/usr/bin/env bash
# Builds without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Usage: ./build.sh [install]
# Env: VERSION (e.g. 0.1.0), KEYSTORE + KEYSTORE_PASSWORD (default: the release key in ~/.android if present, else the debug key).
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-37.0/android.jar
OUT=build
VERSION=${VERSION:-0.0.0}
IFS=. read -r MAJ MIN PAT <<< "$VERSION"
VERSION_CODE=$((MAJ * 10000 + MIN * 100 + PAT))
if [[ -z "${KEYSTORE:-}" && -f "$HOME/.android/nova-release.jks" ]]; then
  KEYSTORE=$HOME/.android/nova-release.jks
  KEYSTORE_PASSWORD=$(cat "$HOME/.android/nova-release.pass")
fi
KEYSTORE=${KEYSTORE:-$HOME/.android/debug.keystore}
KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-android}

rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml \
  --min-sdk-version 33 --target-sdk-version 35 --version-code "$VERSION_CODE" --version-name "$VERSION" \
  -o "$OUT/unsigned.apk" "$OUT/res.zip"

javac -nowarn --release 17 -classpath "$JAR" -d "$OUT/classes" $(find src -name '*.java')
"$BT/d8" --release --min-api 33 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)

"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
printf "%s\n" "$KEYSTORE_PASSWORD" | "$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass stdin \
  --out "$OUT/nova-launcher.apk" "$OUT/aligned.apk"
ls -lh "$OUT/nova-launcher.apk"

if [[ "${1:-}" == install ]]; then
  adb install -r "$OUT/nova-launcher.apk"
  adb shell am start -n dev.nova.launcher/.MainActivity
fi
