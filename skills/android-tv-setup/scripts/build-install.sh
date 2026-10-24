#!/usr/bin/env bash
# Build the launcher, install it on the TV and make it the default Home.
# Run from the TV's work directory (tvs/<model>/). On first run the launcher source is
# copied there from the skill, so each TV keeps its own config.xml.
#   build-install.sh            # build + install + set Home
#   build-install.sh --no-home  # build + install only
set -euo pipefail
export MSYS_NO_PATHCONV=1  # Git Bash on Windows: keep /data/local/tmp as is

SKILL="$(cd "$(dirname "$0")/.." && pwd)"
LAUNCHER="${LAUNCHER_DIR:-$PWD/launcher}"
if [ ! -d "$LAUNCHER" ]; then
  mkdir -p "$LAUNCHER"
  (cd "$SKILL/launcher" && tar cf - --exclude=build --exclude=.gradle --exclude=local.properties .) | (cd "$LAUNCHER" && tar xf -)
  echo "copied launcher source to $LAUNCHER - edit app/src/main/res/values/config.xml there"
fi

# TV address
if [ -n "${TV_HOST:-}" ]; then TV="$TV_HOST:${TV_PORT:-5555}"; else TV="$(head -1 .tv)"; fi

# adb
ADB_BIN="${ADB:-}"
[ -z "$ADB_BIN" ] && ADB_BIN="$(command -v adb || true)"
for c in "${LOCALAPPDATA:-}/Android/Sdk/platform-tools/adb.exe" "$HOME/Library/Android/sdk/platform-tools/adb" "$HOME/Android/Sdk/platform-tools/adb"; do
  [ -z "$ADB_BIN" ] && [ -f "$c" ] && ADB_BIN="$c"
done
[ -n "$ADB_BIN" ] || { echo "adb not found"; exit 1; }
ADB="$ADB_BIN -s $TV"

# Android SDK for Gradle
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
for c in "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
  [ -z "$SDK" ] && [ -d "$c" ] && SDK="$c"
done
[ -n "$SDK" ] || { echo "Android SDK not found: set ANDROID_HOME"; exit 1; }
command -v cygpath >/dev/null && SDK="$(cygpath -m "$SDK")"
echo "sdk.dir=$SDK" > "$LAUNCHER/local.properties"

# JDK 17+: a JRE has no javac; fall back to the JDK bundled with Android Studio.
if ! ls "${JAVA_HOME:-/nonexistent}"/bin/javac* >/dev/null 2>&1; then
  for c in "/c/Program Files/Android/Android Studio/jbr" "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "/opt/android-studio/jbr"; do
    [ -d "$c" ] && export JAVA_HOME="$c" && break
  done
fi

echo "== build"
(cd "$LAUNCHER" && ./gradlew assembleDebug -q --console=plain)
APK="$LAUNCHER/app/build/outputs/apk/debug/app-debug.apk"
command -v cygpath >/dev/null && APK="$(cygpath -m "$APK")"

echo "== install on $TV"
"$ADB_BIN" connect "$TV" >/dev/null
$ADB push "$APK" /data/local/tmp/launcher.apk >/dev/null
# pm install over the shell: adb install can hang on Android 14 waiting for an on-screen confirmation.
$ADB shell pm install -r /data/local/tmp/launcher.apk
$ADB shell rm /data/local/tmp/launcher.apk

if [ "${1:-}" != "--no-home" ]; then
  echo "== set Home"
  $ADB shell cmd package set-home-activity com.cleantv.launcher/.MainActivity
  $ADB shell input keyevent KEYCODE_HOME
  sleep 2
  # set-home-activity can print Success and change nothing; this line is the real answer.
  $ADB shell "dumpsys activity activities | grep -m1 mFocusedApp"
fi
