#!/usr/bin/env bash
# Builds without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Usage: ./build.sh [install]
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-37.0/android.jar
OUT=build

rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml -A assets \
  --min-sdk-version 33 --target-sdk-version 35 --version-code 1 --version-name 1.0 \
  -o "$OUT/unsigned.apk" "$OUT/res.zip"

javac -nowarn --release 17 -classpath "$JAR" -d "$OUT/classes" $(find src -name '*.java')
"$BT/d8" --release --min-api 33 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)

"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
  --out "$OUT/nova-launcher.apk" "$OUT/aligned.apk"
ls -lh "$OUT/nova-launcher.apk"

if [[ "${1:-}" == install ]]; then
  adb install -r "$OUT/nova-launcher.apk"
  adb shell am start -n dev.nova.launcher/.MainActivity
fi
