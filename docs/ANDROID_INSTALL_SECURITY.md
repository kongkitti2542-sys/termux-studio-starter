# Android installation and Play Protect

Gate 0 builds a **debug APK** for developer testing. It is signed with the Android debug certificate, not a stable production publisher identity.

Google Play Protect may display an **unknown developer** warning for APKs installed outside Google Play. A green GitHub Actions run, a verified APK signature, and a SHA-256 digest do **not** prove the APK is malware-free, and they do not guarantee Play Protect approval.

## Verify the build

1. Open the latest successful **Android Debug APK** workflow run for the intended commit.
2. Download its `termux-studio-debug-apk` artifact.
3. Review `apk-signature-report.txt`, `apk-permissions-report.txt`, and `apk-sha256.txt` alongside the APK.
4. Verify that the source commit matches the intended reviewed branch.
5. If Play Protect blocks installation, **do not disable Play Protect** or bypass the warning. Treat this as a device-side safety gate; investigate the exact warning and distribution/signing strategy.

## Production distribution

For a real release, create and securely back up a **persistent release signing key** under owner control; never commit it to Git or generate a new key for every build. Configure signing using protected CI secrets or a trusted offline signing process, audit dependencies and permissions, and consider Play Console distribution / Play Protect review. Release signing alone does not guarantee removal of a warning.

The Gate 0 app has no Termux shell integration, no Codex authentication, and no privileged Android permissions.
