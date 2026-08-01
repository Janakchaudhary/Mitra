# Persistent APK signing for Mitra

## Why this is required

GitHub-hosted runners are temporary. If `assembleDebug` uses the runner's generated
`~/.android/debug.keystore`, APKs produced by separate runs can have different
signing certificates. Android will then reject the newer APK as an update.

Mitra's main/dispatch GitHub workflow therefore refuses to publish an installable
artifact until one persistent signing key has been configured.

## One-time setup

Open a **trusted GitHub Codespace** for this repository and run:

```bash
./scripts/setup-github-signing.sh
```

The script:

1. creates `.signing/mitra-signing.jks`;
2. stores its Base64 form in `MITRA_KEYSTORE_BASE64` GitHub Actions secret;
3. stores the password and alias as GitHub Actions secrets;
4. prints the signing certificate SHA-256 fingerprint.

The workflow expects these repository secrets:

- `MITRA_KEYSTORE_BASE64`
- `MITRA_KEYSTORE_PASSWORD`
- `MITRA_KEY_ALIAS`
- `MITRA_KEY_PASSWORD`

## Back up the key

Download `.signing/mitra-signing.jks` from the Codespace and keep it in a secure
backup location. Do **not** commit it. Do not lose it. GitHub secrets are not a
replacement for your own backup because their values cannot be retrieved later.

## Migration from older Milestone APKs

Older GitHub builds used ephemeral debug signing. Their private runner key is not
available now, so Android cannot update those installs with the new stable key.

Do this once:

1. Back up anything you need manually.
2. Uninstall the old Mitra APK.
3. Run the signed GitHub workflow after completing the one-time setup above.
4. Install the `mitra-update-apk` artifact.

After that, **do not uninstall between versions**. Download future
`mitra-update-apk` artifacts and install them over the existing app. The package
ID remains `com.mitra.learning`, the version code increases, and the signing key
remains stable.

## PR builds

Pull-request builds still compile/test using the runner's normal debug key, but
no phone-installable artifact is uploaded from PR jobs. Only `main`/manual builds
produce the `mitra-update-apk` artifact using the persistent key.

## Windows Command Prompt / PowerShell

You do **not** need Bash, WSL, or `chmod` on Windows.

From the latest Mitra project root in **Command Prompt**, run:

```bat
scripts\setup-github-signing.cmd
```

Or from PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-github-signing.ps1
```

The Windows script:

1. Finds `keytool.exe` from `PATH`, `JAVA_HOME`, or the common Android Studio JBR location.
2. Verifies GitHub CLI (`gh`) login and repository access.
3. Creates `.signing\mitra-signing.jks` if it does not already exist.
4. Reuses (never silently replaces) an existing Mitra signing key.
5. Creates the four GitHub Actions secrets required by the Android workflow.
6. Prints the certificate SHA-256 fingerprint and backup instructions.

The default repository is `Janakchaudhary/Mitra`. To target another repository:

```bat
scripts\setup-github-signing.cmd -Repository owner/repository
```

If `gh` is missing, install GitHub CLI first:

```bat
winget install --id GitHub.cli
```

Then reopen Command Prompt and authenticate:

```bat
gh auth login
```
