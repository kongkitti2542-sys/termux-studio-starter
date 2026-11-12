# Termux Studio — Gate 0 UI scaffold

Native Android Kotlin / Jetpack Compose starter, targeting Android 15. This is **not a finished APK**. It contains a navigation shell with Home, Files, Terminal, Git, and AI placeholders. It does not execute shell commands, access Termux private storage, or read ChatGPT credentials.

## Build
Use Android Studio with Android SDK 35, JDK 17 and Gradle compatible with AGP 8.9.2. This source archive intentionally does not ship a Gradle wrapper binary; generate a trusted wrapper locally, then run `./gradlew assembleDebug`. Verify dependencies and compatibility before distribution.

## Integration gate
1. Validate supported Termux command integration using the official Termux RUN_COMMAND intent/API and its permission and configuration requirements; do not assume interactive PTY access is supported.
2. Decide on secure, opt-in localhost bridge if a full interactive terminal is needed; enforce origin/authentication and explicit command approval.
3. Add a real terminal emulator, PTY transport, session lifecycle, and keyboard handling only after a feasibility test.
4. Implement project-scoped file operations, Git diffs, and Codex CLI execution through the approved bridge.
5. Test on Android 15 and build signed APK in CI only after review.

Never use root, accessibility automation, private-directory bypasses, or embedded ChatGPT session cookies.
