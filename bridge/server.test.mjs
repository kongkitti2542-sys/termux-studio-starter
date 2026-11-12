import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createBridgeServer } from './server.mjs';

test('health requires bearer token, rejects browser origins and has no command endpoint', async () => {
  const server = createBridgeServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const port = server.address().port;
    const url = 'http://127.0.0.1:' + port;
    const request = (path, headers = {}) => fetch(url + path, { headers });
    assert.equal((await request('/v1/health')).status, 401);
    const headers = { Authorization: 'Bearer ' + process.env.TERMUX_STUDIO_TOKEN };
    assert.equal((await request('/v1/health', headers)).status, 200);
    assert.equal((await request('/v1/health', { ...headers, Origin: 'http://evil.test' })).status, 403);
    assert.equal((await request('/v1/command', headers)).status, 404);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});
