import assert from 'node:assert/strict';
import test from 'node:test';

import {
  computeCooldownRemaining,
  getServiceRequestReasonMeta,
  getServiceRequestStatusMeta,
  isServiceRequestOverdue,
  SERVICE_REQUEST_REASONS,
  SERVICE_REQUEST_SLA_MS,
  SERVICE_REQUEST_STATUSES,
  sortServiceRequestQueue,
  validateServiceRequestInput,
} from '../../src/entities/service-request/lib/serviceRequestMeta.js';

test('defines all 6 quick service request reasons with vi/en labels', () => {
  assert.deepEqual(
    SERVICE_REQUEST_REASONS.map((item) => item.code),
    ['WATER_ICE', 'UTENSILS_NAPKINS', 'CLEAN_TABLE', 'ORDER_ASSISTANCE', 'REQUEST_BILL', 'OTHER'],
  );
  assert.equal(getServiceRequestReasonMeta('WATER_ICE', 'vi').label, 'Thêm nước / Đá');
  assert.equal(getServiceRequestReasonMeta('WATER_ICE', 'en').label, 'Water / Ice');
  assert.equal(getServiceRequestStatusMeta('CLAIMED', 'en').label, 'Claimed');
});

test('validateServiceRequestInput enforces OTHER minimum length and 200-char max length', () => {
  assert.equal(
    validateServiceRequestInput({ reason: 'OTHER', note: ' a ' }).valid,
    false,
  );
  assert.equal(
    validateServiceRequestInput({ reason: 'OTHER', note: ' xin đá ' }).valid,
    true,
  );
  assert.equal(
    validateServiceRequestInput({ reason: 'WATER_ICE', note: '' }).valid,
    true,
  );
  assert.equal(
    validateServiceRequestInput({ reason: 'WATER_ICE', note: 'x'.repeat(201) }).valid,
    false,
  );
});

test('isServiceRequestOverdue marks open requests >= 5 minutes old as overdue', () => {
  const now = Date.parse('2026-11-22T12:10:00.000Z');
  const underSla = Date.parse('2026-11-22T12:05:01.000Z');
  const exactSla = now - SERVICE_REQUEST_SLA_MS;

  assert.equal(
    isServiceRequestOverdue({ status: SERVICE_REQUEST_STATUSES.PENDING, createdAt: underSla }, now),
    false,
  );
  assert.equal(
    isServiceRequestOverdue({ status: SERVICE_REQUEST_STATUSES.PENDING, createdAt: exactSla }, now),
    true,
  );
  assert.equal(
    isServiceRequestOverdue({ status: SERVICE_REQUEST_STATUSES.CLAIMED, createdAt: exactSla }, now),
    true,
  );
  assert.equal(
    isServiceRequestOverdue({ status: SERVICE_REQUEST_STATUSES.RESOLVED, createdAt: exactSla }, now),
    false,
  );
  assert.equal(
    isServiceRequestOverdue({ status: SERVICE_REQUEST_STATUSES.CANCELLED, createdAt: exactSla }, now),
    false,
  );
});

test('computeCooldownRemaining returns rounded remaining seconds or 0 when expired', () => {
  const now = 1_700_000_000_000;
  assert.equal(computeCooldownRemaining(now + 41_200, now), 42);
  assert.equal(computeCooldownRemaining(now - 1000, now), 0);
  assert.equal(computeCooldownRemaining(0, now), 0);
});

test('sortServiceRequestQueue sorts open FIFO and history descending', () => {
  const items = [
    { id: 2, createdAt: '2026-11-22T12:05:00.000Z' },
    { id: 1, createdAt: '2026-11-22T12:01:00.000Z' },
    { id: 3, createdAt: '2026-11-22T12:09:00.000Z' },
  ];

  assert.deepEqual(
    sortServiceRequestQueue(items, 'OPEN').map((item) => item.id),
    [1, 2, 3],
  );
  assert.deepEqual(
    sortServiceRequestQueue(items, 'RESOLVED').map((item) => item.id),
    [3, 2, 1],
  );
});
