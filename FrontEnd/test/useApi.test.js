import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import React from 'react';
import { act, create } from 'react-test-renderer';
import { useApi } from '../src/useApi.js';

const originalFetch = globalThis.fetch;
let renderer;
afterEach(() => {
  if (renderer) act(() => renderer.unmount());
  renderer = undefined;
  globalThis.fetch = originalFetch;
});

function mount(paths, deps = [], options) {
  let state;
  const requests = [];
  globalThis.fetch = (url, init) => new Promise((resolve, reject) => {
    requests.push({ url, signal: init.signal, resolve: (value) => resolve(new Response(JSON.stringify(value))), reject });
  });
  function Probe(props) {
    state = useApi(props.paths, props.deps, props.options);
    return null;
  }
  act(() => { renderer = create(React.createElement(Probe, { paths, deps, options })); });
  return {
    requests,
    get state() { return state; },
    update(paths, deps = [], options) {
      act(() => renderer.update(React.createElement(Probe, { paths, deps, options })));
    },
  };
}

async function resolve(request, value) {
  await act(async () => { request.resolve(value); });
}

test('a changed path reloads without explicit dependencies and ignores a late response', async () => {
  const hook = mount('/players/1');
  hook.update('/players/2');
  assert.equal(hook.requests[0].signal.aborted, true);
  await resolve(hook.requests[1], { id: 2 });
  assert.deepEqual(hook.state.data, { id: 2 });
  await resolve(hook.requests[0], { id: 1 });
  assert.deepEqual(hook.state.data, { id: 2 });
  assert.equal(hook.state.loading, false);
});

test('an obsolete rejection cannot clear loading or set an error for the new request', async () => {
  const hook = mount('/old');
  hook.update('/new');
  await act(async () => { hook.requests[0].reject(new Error('old failure')); });
  assert.equal(hook.state.loading, true);
  assert.equal(hook.state.error, null);
  await resolve(hook.requests[1], 'new');
  assert.equal(hook.state.data, 'new');
});

test('parallel responses preserve endpoint order and reload replaces old data', async () => {
  const hook = mount(['/teams', '/players']);
  await resolve(hook.requests[1], ['player']);
  assert.equal(hook.state.loading, true);
  await resolve(hook.requests[0], ['team']);
  assert.deepEqual(hook.state.data, [['team'], ['player']]);
  act(() => hook.state.reload());
  assert.equal(hook.state.data, null);
  assert.equal(hook.state.loading, true);
  assert.equal(hook.requests.length, 4);
  await resolve(hook.requests[2], []);
  await resolve(hook.requests[3], []);
  assert.deepEqual(hook.state.data, [[], []]);
});

test('disabling clears stale data and cancels pending requests; enabling fetches again', async () => {
  const hook = mount('/teams');
  await resolve(hook.requests[0], ['team']);
  hook.update('/teams', [], { enabled: false });
  assert.equal(hook.state.data, null);
  assert.equal(hook.state.loading, false);
  assert.equal(hook.requests[0].signal.aborted, true);
  hook.update('/teams', [], { enabled: true });
  assert.equal(hook.requests.length, 2);
  assert.equal(hook.state.loading, true);
});

test('explicit dependency changes refresh unchanged paths', () => {
  const hook = mount('/teams', [1]);
  hook.update('/teams', [2]);
  assert.equal(hook.requests.length, 2);
  assert.equal(hook.requests[0].signal.aborted, true);
});

test('unmount cancels in-flight requests', () => {
  const hook = mount('/teams');
  act(() => renderer.unmount());
  renderer = undefined;
  assert.equal(hook.requests[0].signal.aborted, true);
});

test('an endpoint error is exposed and cancels the remaining parallel requests', async () => {
  const hook = mount(['/teams', '/players']);
  await act(async () => { hook.requests[0].reject(new Error('offline')); });
  assert.match(hook.state.error, /Could not reach the backend/);
  assert.equal(hook.state.loading, false);
  assert.equal(hook.requests[1].signal.aborted, true);
});
