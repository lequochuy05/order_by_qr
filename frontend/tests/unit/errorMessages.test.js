import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildErrorMessage,
  getErrorDetails,
  getErrorPayload,
  translateErrorMessage,
} from '../../src/shared/lib/errorMessages.js';

test('formats network errors consistently', () => {
  assert.equal(buildErrorMessage(new Error('Network Error')), 'Không thể kết nối đến máy chủ!');
});

test('omits technical details for toast messages', () => {
  const error = {
    message: 'Invalid input data',
    status: 400,
    code: 'VALIDATION_ERROR',
    details: { restaurantName: 'Tên quán không được để trống' },
  };
  assert.equal(
    buildErrorMessage(error, { includeDetails: false }),
    'Dữ liệu đầu vào không hợp lệ.',
  );
});

test('extracts validation details', () => {
  assert.deepEqual(
    getErrorDetails({
      details: { restaurantName: 'Tên quán không được để trống' },
    }),
    { restaurantName: 'Tên quán không được để trống' },
  );
});

test('translates menu option maximum errors', () => {
  assert.equal(
    translateErrorMessage('Max selection cannot exceed option value count', 'BUSINESS_ERROR'),
    'Số lựa chọn tối đa không được lớn hơn số giá trị hiện có.',
  );
});

test('translates duplicate option value errors with their value', () => {
  assert.equal(
    translateErrorMessage('Duplicate option value: Size M', 'BUSINESS_ERROR'),
    'Giá trị tùy chọn bị trùng: Size M',
  );
});

test('uses Vietnamese message from a known error code', () => {
  assert.equal(
    buildErrorMessage(
      {
        message: 'Inventory item name already exists',
        code: 'INVENTORY_ITEM_NAME_EXISTS',
      },
      { includeDetails: false },
    ),
    'Tên nguyên liệu đã tồn tại.',
  );
});

test('does not expose unknown English backend messages', () => {
  assert.equal(
    translateErrorMessage('Unexpected provider failure without a public mapping'),
    'Có lỗi xảy ra, vui lòng thử lại.',
  );
});

test('preserves an existing Vietnamese backend message', () => {
  assert.equal(translateErrorMessage('Số lượng không hợp lệ'), 'Số lượng không hợp lệ');
});

test('translates validation field labels and minimum rules', () => {
  const message = buildErrorMessage({
    message: 'Invalid input data',
    status: 400,
    code: 'VALIDATION_ERROR',
    details: { maxSelection: 'Max selection must be at least 1' },
  });
  assert.match(message, /Số lựa chọn tối đa:/);
  assert.match(message, /phải ít nhất là 1/);
});

test('returns translated details for direct form rendering', () => {
  assert.deepEqual(
    getErrorDetails({
      details: {
        restaurantName: 'Restaurant name cannot be empty',
        restaurantEmail: 'Restaurant email is invalid',
      },
    }),
    {
      restaurantName: 'Tên nhà hàng không được để trống.',
      restaurantEmail: 'Email nhà hàng không hợp lệ.',
    },
  );
});

test('parses a JSON response payload', () => {
  assert.deepEqual(
    getErrorPayload({
      response: { data: '{"code":"TABLE_NOT_FOUND","message":"Table ID invalid"}' },
    }),
    { code: 'TABLE_NOT_FOUND', message: 'Table ID invalid' },
  );
});

test('returns a plain string payload when it is not JSON', () => {
  assert.equal(getErrorPayload('plain failure'), 'plain failure');
});

test('translates known table QR errors', () => {
  assert.match(translateErrorMessage('Secure Table Code invalid'), /Không tìm thấy thông tin bàn/);
});

test('translates payment in progress errors', () => {
  assert.equal(
    translateErrorMessage('The table is currently being paid. Please contact a staff member.'),
    'Bàn đang trong quá trình thanh toán. Vui lòng liên hệ nhân viên.',
  );
});
