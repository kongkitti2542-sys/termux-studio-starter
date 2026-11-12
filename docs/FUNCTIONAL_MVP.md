# Termux Studio — Functional MVP trial

This is an **experimental command-and-file bridge**, not a full terminal emulator. Shell jobs are non-interactive (no PTY or stdin); interactive Codex login / TUI cannot work in this build. Use non-interactive commands such as `pwd`, `git status`, or an already-authenticated `codex exec "..."`. Do not run untrusted commands.

## Setup on the same Android phone

In Termux:
```sh
pkg install nodejs git
git clone https://github.com/kongkitti2542-sys/termux-studio-starter.git
cd termux-studio-starter
git switch feature/functional-mvp
cd bridge
export TERMUX_STUDIO_TOKEN="$(node -e 'process.stdout.write(require("node:crypto").randomBytes(32).toString("hex"))')"
printf 'Pairing token: %s\n' "$TERMUX_STUDIO_TOKEN"
node server.mjs
```

Keep this Termux session running. Open the APK built from the **feature/functional-mvp** branch, paste the token on Home and press Connect. The token is stored in the APK's private preferences (not encrypted); do not reuse valuable credentials. The app communicates over loopback only. Termux may stop the service when Android kills background processes.

## Available

- Health/pairing with random bearer token
- Browse a workspace under Termux home, open and save existing text files up to 128 KiB
- Execute an explicitly confirmed non-interactive shell job, poll its bounded output, request cancellation
- Use Git through commands entered in the Git tab; non-interactive Codex through commands entered in the AI tab

## Limitations / security

- Not a PTY. No full-screen terminal, stdin, ANSI renderer, session resume or interactive Codex TUI.
- A paired APK can execute arbitrary shell commands as the Termux user. Only pair your own app, and never paste tokens into chats or repositories.
- Cancellation sends SIGTERM to the parent process; descendant processes may remain alive.
- Only files under the configured workspace root are accessible through the file API; command jobs are **not sandboxed** and can access anything Termux itself can.
- Android stores the token in app-private SharedPreferences without hardware-backed encryption. Re-pair with a new token if exposed.
- HTTP is cleartext on loopback; another local app may attempt to connect, so token secrecy is essential.
- No root, external network binding, or direct access to Codex credentials.
- The project must pass CI and real-device tests before merging.

## Acceptance test

1. Open Home, paste token, Connect → Bridge ready.
2. Files → Browse `.` → open and save a small existing text file.
3. Terminal → run `pwd` → confirm → output and exit code.
4. Git → run `git --version` → confirm → output.
5. Stop a long-running job and verify behavior. Do not assume child processes were killed.
6. Test reconnect after restarting Termux server with a new token.

Do not merge into main until the owner approves.
