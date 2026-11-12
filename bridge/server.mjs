import http from 'node:http';
import { randomBytes, timingSafeEqual } from 'node:crypto';

const HOST = '127.0.0.1';
const PORT = Number(process.env.TERMUX_STUDIO_PORT || 8765);
const TOKEN = process.env.TERMUX_STUDIO_TOKEN;
if (!TOKEN || TOKEN.length < 32) {
  console.error('Set TERMUX_STUDIO_TOKEN to a random secret of at least 32 characters.');
  process.exit(1);
}
if (!Number.isInteger(PORT) || PORT < 1024 || PORT > 65535) throw new Error('Invalid port');

function authorized(value) {
  if (typeof value !== 'string') return false;
  const supplied = Buffer.from(value, 'utf8');
  const expected = Buffer.from('Bearer ' + TOKEN, 'utf8');
  return supplied.length === expected.length && timingSafeEqual(supplied, expected);
}

export function createBridgeServer() {
  return http.createServer((req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    const send = (status, payload) => {
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(payload));
    };
    // Reject browser-originated cross-origin requests, including requests with Origin: null.
    if (req.headers.origin !== undefined || req.headers['sec-fetch-site'] !== undefined) {
      return send(403, { error: 'browser_requests_disabled' });
    }
    if (!authorized(req.headers.authorization)) return send(401, { error: 'unauthorized' });
    if (req.method !== 'GET' || req.url !== '/v1/health') return send(404, { error: 'not_found' });
    return send(200, { service: 'termux-studio-bridge', protocol: 1, status: 'ready', capabilities: ['health'] });
  });
}
if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  createBridgeServer().listen(PORT, HOST, () => console.log('Bridge listening on ' + HOST + ':' + PORT));
}
