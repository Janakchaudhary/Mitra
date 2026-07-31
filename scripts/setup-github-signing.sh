#!/usr/bin/env bash
set -euo pipefail

# Run ONCE from a trusted GitHub Codespace for this repository.
# It creates a long-lived personal signing key, stores an encoded copy in
# GitHub Actions secrets, and leaves a local backup that you MUST download and
# keep somewhere safe. Never commit the .jks file.

command -v keytool >/dev/null || { echo "keytool is required (Java/JDK)." >&2; exit 1; }
command -v gh >/dev/null || { echo "GitHub CLI (gh) is required." >&2; exit 1; }

gh auth status >/dev/null

SIGNING_DIR=".signing"
KEYSTORE_FILE="$SIGNING_DIR/mitra-signing.jks"
ALIAS="mitra"

mkdir -p "$SIGNING_DIR"
chmod 700 "$SIGNING_DIR"

if [ -e "$KEYSTORE_FILE" ]; then
  echo "Refusing to overwrite existing $KEYSTORE_FILE"
  echo "If that is your real signing key, keep using it. Losing/replacing it breaks APK updates."
  exit 1
fi

printf 'Create a password for the Mitra signing key (minimum 6 characters): '
read -r -s SIGNING_PASSWORD
printf '\n'

if [ ${#SIGNING_PASSWORD} -lt 6 ]; then
  echo "Password must be at least 6 characters." >&2
  exit 1
fi

keytool -genkeypair \
  -keystore "$KEYSTORE_FILE" \
  -storetype PKCS12 \
  -storepass "$SIGNING_PASSWORD" \
  -keypass "$SIGNING_PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 3072 \
  -validity 10000 \
  -dname "CN=Mitra Personal App, OU=Personal, O=Mitra, C=IN"

chmod 600 "$KEYSTORE_FILE"

BASE64_VALUE="$(base64 -w 0 "$KEYSTORE_FILE")"
printf '%s' "$BASE64_VALUE" | gh secret set MITRA_KEYSTORE_BASE64
printf '%s' "$SIGNING_PASSWORD" | gh secret set MITRA_KEYSTORE_PASSWORD
printf '%s' "$ALIAS" | gh secret set MITRA_KEY_ALIAS
printf '%s' "$SIGNING_PASSWORD" | gh secret set MITRA_KEY_PASSWORD

unset BASE64_VALUE SIGNING_PASSWORD

echo
echo "Persistent GitHub signing secrets created."
echo "Signing certificate fingerprint:"
keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$ALIAS" 2>/dev/null | grep -E 'SHA256:' | head -1 || true

echo
echo "IMPORTANT: Download and securely back up this file before deleting the Codespace:"
echo "  $KEYSTORE_FILE"
echo
echo "Do NOT commit it to Git. GitHub secrets cannot be downloaded later."
echo "After the first APK signed with this key is installed, all future APK updates must use this same key."
