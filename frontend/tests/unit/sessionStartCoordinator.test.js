import assert from 'node:assert/strict';
import test from 'node:test';

import {
  createSessionStartCoordinator,
  startTableSessionOnce,
} from '../../src/features/customer-ordering/api/sessionStartCoordinator.js';

test('session coordinator shares an in-flight request for the same table', async () => {
  const startOnce = createSessionStartCoordinator();
  let resolveRequest;
  let callCount = 0;
  const request = new Promise((resolve) => {
    resolveRequest = resolve;
  });
  const startSession = () => {
    callCount += 1;
    return request;
  };

  const first = startOnce('BAN_01', startSession);
  const second = startOnce('BAN_01', startSession);

  assert.equal(first, second);
  assert.equal(callCount, 1);
  resolveRequest({ sessionToken: 'token-1' });
  await first;
});

test('session coordinator allows a new request after success', async () => {
  const startOnce = createSessionStartCoordinator();
  let callCount = 0;
  const startSession = async () => ({ call: ++callCount });

  assert.deepEqual(await startOnce('BAN_01', startSession), { call: 1 });
  assert.deepEqual(await startOnce('BAN_01', startSession), { call: 2 });
});

test('session coordinator does not merge requests for different tables', async () => {
  const startOnce = createSessionStartCoordinator();
  let callCount = 0;
  const startSession = async (tableCode) => {
    callCount += 1;
    return tableCode;
  };

  assert.deepEqual(
    await Promise.all([startOnce('BAN_01', startSession), startOnce('BAN_02', startSession)]),
    ['BAN_01', 'BAN_02'],
  );
  assert.equal(callCount, 2);
});

test('session coordinator allows retry after an async failure', async () => {
  const startOnce = createSessionStartCoordinator();
  let callCount = 0;
  const startSession = async () => {
    callCount += 1;
    if (callCount === 1) throw new Error('network error');
    return { sessionToken: 'token-after-retry' };
  };

  await assert.rejects(startOnce('BAN_01', startSession), /network error/);
  assert.deepEqual(await startOnce('BAN_01', startSession), {
    sessionToken: 'token-after-retry',
  });
});

test('session coordinator converts a synchronous throw to a rejected promise', async () => {
  const startOnce = createSessionStartCoordinator();

  await assert.rejects(
    startOnce('BAN_01', () => {
      throw new Error('sync error');
    }),
    /sync error/,
  );
});

test('default session coordinator is a reusable function', () => {
  assert.equal(typeof startTableSessionOnce, 'function');
});
