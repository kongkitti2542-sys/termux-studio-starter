# Gate 2 — Local Bridge, Phase A

## Security contract

This first increment is **health-only**. It does not run shell commands, inspect Termux private files, launch Codex, or expose Git.

- Bind exclusively to `127.0.0.1` (never `0.0.0.0`).
- Require a randomly generated bearer token (at least 32 characters), compared in constant time.
- Reject browser Origin and Sec-Fetch-Site headers. No CORS.
- No unauthenticated status endpoint; no arbitrary path, command, file, or subprocess routes.
- Never commit tokens or expose them in logs. Do not embed tokens into APKs.
- Loopback is **not** an authentication boundary; other local apps may attempt connections.
- A future Android pairing flow must securely provision and store a token before connection.
- Android 15 background execution and terminal PTY behavior remain unverified.

## Run inside Termux (manual, opt-in)

```sh
cd ~/termux-studio-starter/bridge
export TERMUX_STUDIO_TOKEN="$(node -e 'process.stdout.write(require("node:crypto").randomBytes(32).toString("hex"))')"
node --test
node server.mjs
```

Keep the token secret. For testing from a second Termux session, run:

```sh
curl -H "Authorization: Bearer $TERMUX_STUDIO_TOKEN" http://127.0.0.1:8765/v1/health
```

The Android app is **not yet paired** with this server. This increment validates the Termux-side transport before Android integration.

## Acceptance criteria

1. No token → HTTP 401.
2. Valid token → HTTP 200 and capability `health`.
3. Browser Origin → HTTP 403.
4. Any command endpoint → HTTP 404.
5. Service binds to loopback only.
6. Android APK is not granted shell permissions in this phase.

## Next increment

Implement explicit local pairing and Android client connection state, then a constrained PTY session protocol with lifecycle management, user-visible approval, backpressure, and cancellation. No automatic merges or deployments.
