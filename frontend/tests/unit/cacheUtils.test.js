import assert from 'node:assert/strict';
import test from 'node:test';

import { createCachedRequest, createKeyedCachedRequest } from '../../src/shared/lib/cacheUtils.js';

test('cached request shares an in-flight request', async () => {
  let resolveRequest;
  let calls = 0;
  const cache = createCachedRequest(() => {
    calls += 1;
    return new Promise((resolve) => {
      resolveRequest = resolve;
    });
  });

  const first = cache.requestFn();
  const second = cache.requestFn();
  assert.equal(first, second);
  assert.equal(calls, 1);
  resolveRequest('done');
  assert.equal(await first, 'done');
});

test('cached request reuses data before expiration', async () => {
  let calls = 0;
  const cache = createCachedRequest(async () => ++calls, 10_000);

  assert.equal(await cache.requestFn(), 1);
  assert.equal(await cache.requestFn(), 1);
  assert.equal(calls, 1);
});

test('cached request force option bypasses cached data', async () => {
  let calls = 0;
  const cache = createCachedRequest(async () => ++calls, 10_000);

  await cache.requestFn();
  assert.equal(await cache.requestFn({ force: true }), 2);
});

test('cached request clearCache removes stored data', async () => {
  let calls = 0;
  const cache = createCachedRequest(async () => ++calls, 10_000);

  await cache.requestFn();
  cache.clearCache();
  assert.equal(await cache.requestFn(), 2);
});

test('cached request retries after rejection', async () => {
  let calls = 0;
  const cache = createCachedRequest(async () => {
    calls += 1;
    if (calls === 1) throw new Error('failed');
    return 'recovered';
  });

  await assert.rejects(cache.requestFn(), /failed/);
  assert.equal(await cache.requestFn(), 'recovered');
});

test('keyed cache shares requests for the same key', async () => {
  let calls = 0;
  const cache = createKeyedCachedRequest(async (key) => {
    calls += 1;
    return key.toUpperCase();
  });

  assert.deepEqual(await Promise.all([cache.requestFn('a'), cache.requestFn('a')]), ['A', 'A']);
  assert.equal(calls, 1);
});

test('keyed cache keeps different keys isolated', async () => {
  let calls = 0;
  const cache = createKeyedCachedRequest(async (key) => {
    calls += 1;
    return key;
  });

  assert.deepEqual(await Promise.all([cache.requestFn('a'), cache.requestFn('b')]), ['a', 'b']);
  assert.equal(calls, 2);
});

test('keyed cache clearCache removes all keys', async () => {
  let calls = 0;
  const cache = createKeyedCachedRequest(async (key) => `${key}-${++calls}`, 10_000);

  assert.equal(await cache.requestFn('a'), 'a-1');
  assert.equal(await cache.requestFn('a'), 'a-1');
  cache.clearCache();
  assert.equal(await cache.requestFn('a'), 'a-2');
});
