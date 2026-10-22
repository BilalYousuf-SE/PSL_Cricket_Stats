import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import { api, ApiError, verifyAdminPassword } from '../src/api.js';

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

test('custom headers preserve JSON accept and pass through cancellation', async () => {
  const controller = new AbortController();
  globalThis.fetch = async (url, options) => {
    assert.equal(options.headers.Accept, 'application/json');
    assert.equal(options.headers['X-Test'], 'yes');
    assert.equal(options.signal, controller.signal);
    return new Response('{"ok":true}');
  };
  assert.deepEqual(await api.get('/teams', { headers: { 'X-Test': 'yes' }, signal: controller.signal }), { ok: true });
});

test('cancellation remains an AbortError', async () => {
  const error = new DOMException('Aborted', 'AbortError');
  globalThis.fetch = async () => { throw error; };
  await assert.rejects(api.get('/teams'), (caught) => caught === error);
});

test('HTTP failures retain the backend message and status', async () => {
  globalThis.fetch = async () => new Response('{"message":"Player not found"}', { status: 404 });
  await assert.rejects(api.get('/players/123'), (error) => error instanceof ApiError && error.status === 404 && error.message === 'Player not found');
});

test('empty success responses return null', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 });
  assert.equal(await api.get('/teams'), null);
});

test('admin sign-in verifies the supplied password with the backend', async () => {
  const controller = new AbortController();
  globalThis.fetch = async (url, options) => {
    assert.ok(url.endsWith('/admin/auth'));
    assert.equal(options.method, 'POST');
    assert.equal(options.headers['X-Admin-Password'], 'test-password');
    assert.equal(options.signal, controller.signal);
    return new Response(null, { status: 204 });
  };
  assert.equal(await verifyAdminPassword('test-password', { signal: controller.signal }), null);
  globalThis.fetch = async () => new Response('{"message":"Wrong or missing admin password"}', { status: 401 });
  await assert.rejects(verifyAdminPassword('wrong'), (error) => error.status === 401);
});
