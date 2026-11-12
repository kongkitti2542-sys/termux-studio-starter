# Codex chat improvements — Hermux reference

Reference: https://github.com/omarqaterge/hermes-mobile-app (public README; MIT repository).
Hermux documents chat history, live tool cards, approvals, and session handling. This is a design reference, **not a copied implementation**. No third-party source files have been imported.

## Implemented in this branch

- Dedicated `POST /v1/codex` authenticated endpoint.
- Run installed Codex CLI using `spawn('codex', argv)` (not a shell), with `--json` event output.
- Capture `thread.started.thread_id`, and use `codex exec --json resume SESSION_ID PROMPT` for subsequent messages.
- Stream bounded output and sanitized activity metadata through existing job polling.
- Android AI tab shows chat text, session ID, latest activity, and New Chat.

## Limitations

- Not a PTY and not a complete event-by-event renderer.
- Polling is near-live, not WebSocket.
- Android state is in-memory and resets when app process is killed; Codex session files remain under its own home.
- No background notification or durable queue.
- Existing generic command endpoint remains powerful and requires explicit user confirmation in the app.
- No Codex auth tokens are read by the bridge.
- No automatic merge, deploy, or permission escalation.

## Device acceptance

1. Verify `codex --version` and `codex login status` in Termux.
2. Restart the Bridge from this branch and reconnect APK.
3. Send first message and verify `thread.started` session ID.
4. Send a follow-up and verify context continuity.
5. Inspect activity, stop handling, and network disconnection.
6. Review CI build and actual Android behavior before approval.

## Security follow-up

The generic command route needs a stronger session-specific capability model and process-group cancellation before release. No claim of full security audit is made.
