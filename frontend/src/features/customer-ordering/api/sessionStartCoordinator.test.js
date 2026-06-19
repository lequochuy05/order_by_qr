import assert from 'node:assert/strict';
import test from 'node:test';

import { createSessionStartCoordinator } from './sessionStartCoordinator.js';

test('shares an in-flight start request for the same table', async () => {
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

  await startOnce('BAN_01', async () => {
    callCount += 1;
    return { sessionToken: 'token-2' };
  });

  assert.equal(callCount, 2);
});

test('does not merge requests for different tables', async () => {
  const startOnce = createSessionStartCoordinator();
  let callCount = 0;
  const startSession = async (tableCode) => {
    callCount += 1;
    return tableCode;
  };

  const results = await Promise.all([
    startOnce('BAN_01', startSession),
    startOnce('BAN_02', startSession),
  ]);

  assert.deepEqual(results, ['BAN_01', 'BAN_02']);
  assert.equal(callCount, 2);
});

test('allows retry after a failed request', async () => {
  const startOnce = createSessionStartCoordinator();
  let callCount = 0;
  const startSession = async () => {
    callCount += 1;
    if (callCount === 1) {
      throw new Error('network error');
    }
    return { sessionToken: 'token-after-retry' };
  };

  await assert.rejects(startOnce('BAN_01', startSession), /network error/);
  const result = await startOnce('BAN_01', startSession);

  assert.equal(result.sessionToken, 'token-after-retry');
  assert.equal(callCount, 2);
});
