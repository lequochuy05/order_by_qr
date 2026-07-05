import assert from 'node:assert/strict';
import test from 'node:test';

import {
  getSystemErrorContent,
  getSystemErrorStatusCodes,
  normalizeSystemErrorKey,
} from '../../src/shared/lib/systemErrors.js';

test('defines shared pages for common HTTP and app errors', () => {
  const codes = getSystemErrorStatusCodes();

  [
    '400',
    '401',
    '403',
    '404',
    '408',
    '409',
    '410',
    '422',
    '429',
    '500',
    '502',
    '503',
    '504',
    'offline',
    'runtime',
  ].forEach((code) => assert.ok(codes.includes(code), `${code} should be configured`));
});

test('keeps the 403 page focused on permission problems', () => {
  const content = getSystemErrorContent(403);

  assert.equal(content.statusCode, '403');
  assert.match(content.title, /Không có quyền/);
  assert.equal(content.primaryAction.to, '/admin/dashboard');
});

test('keeps unknown routes on the shared 404 treatment', () => {
  const content = getSystemErrorContent('404');

  assert.equal(content.statusCode, '404');
  assert.match(content.message, /Đường dẫn/);
  assert.equal(content.primaryAction.to, '/');
});

test('normalizes network errors to the offline page', () => {
  assert.equal(normalizeSystemErrorKey('network'), 'offline');
  assert.equal(getSystemErrorContent('network').statusCode, 'OFFLINE');
});

test('falls back gracefully for uncommon status codes', () => {
  const content = getSystemErrorContent(418);

  assert.equal(content.statusCode, '418');
  assert.match(content.title, /Không thể xử lý/);
  assert.equal(content.primaryAction.type, 'reload');
});
