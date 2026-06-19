import assert from 'node:assert/strict';
import test from 'node:test';

import { buildErrorMessage, getErrorDetails } from './errorMessages.js';

test('formats network errors consistently', () => {
  assert.equal(buildErrorMessage(new Error('Network Error')), 'Không thể kết nối đến máy chủ!');
});

test('can omit technical details for toast messages', () => {
  const error = {
    message: 'Invalid input data',
    status: 400,
    code: 'VALIDATION_ERROR',
    details: { restaurantName: 'Tên quán không được để trống' },
  };

  assert.equal(buildErrorMessage(error, { includeDetails: false }), 'Invalid input data');
  assert.deepEqual(getErrorDetails(error), {
    restaurantName: 'Tên quán không được để trống',
  });
});
