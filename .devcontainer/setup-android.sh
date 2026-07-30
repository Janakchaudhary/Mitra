#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="8.11.1"
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"

if ! command -v gradle >/dev/null 2>&1; then
  if [ ! -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    curl -fsSL https://get.sdkman.io | bash
  fi
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk install gradle "$GRADLE_VERSION" || sdk use gradle "$GRADLE_VERSION"
fi

mkdir -p "$ANDROID_HOME/cmdline-tools"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  curl -fL \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    -o "$TMP_DIR/tools.zip"
  unzip -q "$TMP_DIR/tools.zip" -d "$TMP_DIR/unpacked"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$TMP_DIR/unpacked/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --licenses >/dev/null || true
sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

{
  echo "export ANDROID_HOME=\"$ANDROID_HOME\""
  echo "export ANDROID_SDK_ROOT=\"$ANDROID_HOME\""
  echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
} >> "$HOME/.bashrc"

printf '\nMitra development environment ready.\n'
gradle --version | sed -n '1,8p'
sdkmanager --version
printf '\nBuild with: gradle testDebugUnitTest lintDebug assembleDebug\n'
