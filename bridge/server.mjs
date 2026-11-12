import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { spawn } from 'node:child_process';
import { readdir, readFile, writeFile, realpath, stat } from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';

const HOST = '127.0.0.1';
const PORT = Number(process.env.TERMUX_STUDIO_PORT || 8765);
const TOKEN = process.env.TERMUX_STUDIO_TOKEN;
const ROOT = await realpath(process.env.TERMUX_STUDIO_ROOT || os.homedir());
if (!TOKEN || TOKEN.length < 32) throw new Error('TERMUX_STUDIO_TOKEN must have at least 32 characters');
if (!Number.isInteger(PORT) || PORT < 1024 || PORT > 65535) throw new Error('Invalid port');
const jobs = new Map();
let nextJob = 0;
const MAX_OUTPUT = 120000;

function authorized(value) {
  if (typeof value !== 'string') return false;
  const a = Buffer.from(value);
  const b = Buffer.from('Bearer ' + TOKEN);
  return a.length === b.length && timingSafeEqual(a, b);
}
async function resolveInside(relative, forWrite = false) {
  if (typeof relative !== 'string' || relative.includes('\\0') || path.isAbsolute(relative)) throw new Error('invalid_path');
  const target = path.resolve(ROOT, relative || '.');
  if (target !== ROOT && !target.startsWith(ROOT + path.sep)) throw new Error('outside_workspace');
  const actual = await realpath(forWrite ? path.dirname(target) : target);
  if (actual !== ROOT && !actual.startsWith(ROOT + path.sep)) throw new Error('outside_workspace');
  return forWrite ? path.join(actual, path.basename(target)) : actual;
}
async function body(req) {
  let data = '';
  for await (const part of req) {
    data += part.toString('utf8');
    if (data.length > 262144) throw new Error('payload_too_large');
  }
  return JSON.parse(data || '{}');
}
function startJob(command, cwd) {
  if (typeof command !== 'string' || !command.trim() || command.length > 4096) throw new Error('invalid_command');
  if ([...jobs.values()].filter(j => j.running).length >= 2) throw new Error('too_many_jobs');
  const id = String(++nextJob);
  const child = spawn(process.env.SHELL || '/bin/sh', ['-c', command], {
    cwd, env: { ...process.env, TERM: 'dumb' }, stdio: ['ignore', 'pipe', 'pipe']
  });
  const job = { id, command, output: '', running: true, exitCode: null, child };
  jobs.set(id, job);
  const append = chunk => { job.output = (job.output + chunk.toString('utf8')).slice(-MAX_OUTPUT); };
  child.stdout.on('data', append);
  child.stderr.on('data', append);
  child.on('error', e => { append(e.message); job.running = false; job.exitCode = -1; });
  child.on('close', code => { job.running = false; job.exitCode = code; });
  if (jobs.size > 20) for (const [key, val] of jobs) { if (!val.running && key !== id) { jobs.delete(key); break; } }
  return { id };
}

function startCodex(prompt, cwd, sessionId) {
  if (typeof prompt !== 'string' || !prompt.trim() || prompt.length > 16000) throw new Error('invalid_prompt');
  if (sessionId !== undefined && (typeof sessionId !== 'string' || !/^[0-9a-fA-F-]{36}$/.test(sessionId))) throw new Error('invalid_session');
  if ([...jobs.values()].some(j => j.running && j.codex)) throw new Error('codex_busy');
  const id = String(++nextJob);
  const args = sessionId ? ['exec', '--json', 'resume', sessionId, prompt] : ['exec', '--json', prompt];
  const child = spawn('codex', args, { cwd, env: { ...process.env, TERM: 'dumb' }, stdio: ['ignore', 'pipe', 'pipe'] });
  const job = { id, output: '', running: true, exitCode: null, child, codex: true, sessionId: sessionId || null, events: [], buffer: '' };
  jobs.set(id, job);
  child.stdout.on('data', chunk => {
    job.buffer += chunk.toString('utf8');
    const lines = job.buffer.split('\n');
    job.buffer = lines.pop() || '';
    for (const line of lines) {
      try {
        const event = JSON.parse(line);
        if (event.type === 'thread.started' && typeof event.thread_id === 'string') job.sessionId = event.thread_id;
        if (event.type === 'item.completed' && event.item?.type === 'agent_message' && typeof event.item.text === 'string') job.output += event.item.text + '\n';
        if (event.type === 'item.started' || event.type === 'item.completed') {
          const item = event.item || {};
          if (item.type === 'command_execution') job.output += '[' + event.type + '] ' + (item.command || 'command') + '\n';
        }
        if (event.type === 'turn.failed') job.output += '[Codex turn failed]\\n';
        job.events.push({ type: event.type, itemType: event.item?.type || null });
        if (job.events.length > 100) job.events.shift();
        job.output = job.output.slice(-MAX_OUTPUT);
      } catch { /* Ignore malformed output lines, never treat as commands. */ }
    }
  });
  child.stderr.on('data', chunk => { job.output = (job.output + chunk.toString('utf8')).slice(-MAX_OUTPUT); });
  child.on('error', error => { job.output += error.message; job.running = false; job.exitCode = -1; });
  child.on('close', code => { job.running = false; job.exitCode = code; });
  return { id, sessionId: job.sessionId };
}

export function createBridgeServer() {
  return http.createServer(async (req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    const send = (status, value) => { res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' }); res.end(JSON.stringify(value)); };
    if (req.headers.origin !== undefined || req.headers['sec-fetch-site'] !== undefined) return send(403, { error: 'browser_requests_disabled' });
    if (!authorized(req.headers.authorization)) return send(401, { error: 'unauthorized' });
    const url = new URL(req.url, 'http://localhost');
    try {
      if (req.method === 'GET' && url.pathname === '/v1/health') return send(200, { service: 'termux-studio-bridge', protocol: 2, status: 'ready', capabilities: ['files', 'commands', 'jobs'] });
      if (req.method === 'GET' && url.pathname === '/v1/files') {
        const dir = await resolveInside(url.searchParams.get('path') || '.');
        const entries = await readdir(dir, { withFileTypes: true });
        return send(200, { entries: entries.filter(e => !e.isSymbolicLink()).slice(0, 300).map(e => ({ name: e.name, directory: e.isDirectory() })) });
      }
      if (req.method === 'GET' && url.pathname === '/v1/file') {
        const file = await resolveInside(url.searchParams.get('path') || '');
        if ((await stat(file)).size > 131072) throw new Error('file_too_large');
        return send(200, { content: await readFile(file, 'utf8') });
      }
      if (req.method === 'PUT' && url.pathname === '/v1/file') {
        const data = await body(req);
        if (typeof data.content !== 'string' || data.content.length > 131072) throw new Error('invalid_content');
        const file = await resolveInside(data.path, true);
        if (!(await stat(file)).isFile()) throw new Error('not_file');
        await writeFile(file, data.content, { flag: 'w' });
        return send(200, { saved: true });
      }
      if (req.method === 'POST' && url.pathname === '/v1/codex') {
        const data = await body(req);
        return send(202, startCodex(data.prompt, await resolveInside(data.cwd || '.'), data.sessionId));
      }
      if (req.method === 'POST' && url.pathname === '/v1/jobs') {
        const data = await body(req);
        return send(202, startJob(data.command, await resolveInside(data.cwd || '.')));
      }
      const match = url.pathname.match(/^\/v1\/jobs\/(\d+)$/);
      if (match) {
        const job = jobs.get(match[1]);
        if (!job) return send(404, { error: 'not_found' });
        if (req.method === 'DELETE') { if (job.running) job.child.kill('SIGTERM'); return send(200, { cancelled: true }); }
        if (req.method === 'GET') return send(200, { id: job.id, output: job.output, running: job.running, exitCode: job.exitCode, sessionId: job.sessionId || null, events: job.events || [] });
      }
      return send(404, { error: 'not_found' });
    } catch (error) {
      return send(400, { error: error.message || 'request_failed' });
    }
  });
}
if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  createBridgeServer().listen(PORT, HOST, () => console.log('Termux Studio bridge ready on loopback port ' + PORT));
}
