import assert from 'node:assert/strict';
import test from 'node:test';

import { buildErrorMessage, getErrorDetails, translateErrorMessage } from './errorMessages.js';

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

  assert.equal(
    buildErrorMessage(error, { includeDetails: false }),
    'Dữ liệu đầu vào không hợp lệ.',
  );
  assert.deepEqual(getErrorDetails(error), {
    restaurantName: 'Tên quán không được để trống',
  });
});

test('translates menu option business errors', () => {
  assert.equal(
    translateErrorMessage('Max selection cannot exceed option value count', 'BUSINESS_ERROR'),
    'Số lựa chọn tối đa không được lớn hơn số giá trị hiện có.',
  );
  assert.equal(
    translateErrorMessage('Duplicate option value: Size M', 'BUSINESS_ERROR'),
    'Giá trị tùy chọn bị trùng: Size M',
  );
});

test('uses Vietnamese message from error code when backend message is still English', () => {
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

test('does not expose an unknown English backend message to the UI', () => {
  assert.equal(
    translateErrorMessage('Unexpected provider failure without a public mapping'),
    'Có lỗi xảy ra, vui lòng thử lại.',
  );
});

test('translates validation detail labels and messages', () => {
  const message = buildErrorMessage({
    message: 'Invalid input data',
    status: 400,
    code: 'VALIDATION_ERROR',
    details: {
      maxSelection: 'Max selection must be at least 1',
    },
  });

  assert.match(message, /Số lựa chọn tối đa:/);
  assert.match(message, /phải ít nhất là 1/);
  assert.doesNotMatch(message, /Invalid input data/);
});

test('returns translated field details for forms that render backend validation directly', () => {
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
