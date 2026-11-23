import assert from 'node:assert/strict';
import test from 'node:test';

import {
  DAYS_OF_WEEK_ORDER,
  PROMOTION_STATUSES,
  formatPromotionTime,
  getCurrentDayOfWeek,
  isEveryDayPromotion,
  parseTimeToMinutes,
  resolvePromotionStatus,
  sortDaysOfWeek,
  toApiLocalTime,
} from '../../src/entities/promotion/lib/promotionStatus.js';
import { translateErrorMessage } from '../../src/shared/lib/errorMessages.js';

test('parseTimeToMinutes parses HH:mm and HH:mm:ss correctly', () => {
  assert.equal(parseTimeToMinutes('00:00'), 0);
  assert.equal(parseTimeToMinutes('11:30'), 690);
  assert.equal(parseTimeToMinutes('23:59:59'), 1439);
  assert.equal(parseTimeToMinutes('24:00'), null);
  assert.equal(parseTimeToMinutes('invalid'), null);
  assert.equal(parseTimeToMinutes(null), null);
});

test('formatPromotionTime and toApiLocalTime normalize time strings', () => {
  assert.equal(formatPromotionTime('11:30:00'), '11:30');
  assert.equal(formatPromotionTime('09:05'), '09:05');
  assert.equal(formatPromotionTime(''), '');

  assert.equal(toApiLocalTime('11:30'), '11:30:00');
  assert.equal(toApiLocalTime('14:45:30'), '14:45:30');
  assert.equal(toApiLocalTime(''), '');
});

test('sortDaysOfWeek and isEveryDayPromotion normalize weekly schedules', () => {
  assert.deepEqual(sortDaysOfWeek(['SUNDAY', 'MONDAY', 'WEDNESDAY', 'MONDAY']), [
    'MONDAY',
    'WEDNESDAY',
    'SUNDAY',
  ]);
  assert.deepEqual(sortDaysOfWeek(['INVALID', 'FRIDAY']), ['FRIDAY']);

  assert.equal(isEveryDayPromotion([]), true);
  assert.equal(isEveryDayPromotion(DAYS_OF_WEEK_ORDER), true);
  assert.equal(isEveryDayPromotion(['MONDAY', 'FRIDAY']), false);
});

test('resolvePromotionStatus evaluates active flag, day of week, and time window', () => {
  // 2026-11-20 is a FRIDAY
  const fridayNoon = new Date(2026, 10, 20, 12, 30, 0);
  const fridayEvening = new Date(2026, 10, 20, 20, 0, 0);

  assert.equal(getCurrentDayOfWeek(fridayNoon), 'FRIDAY');

  const inactivePromo = {
    active: false,
    startTime: '11:00:00',
    endTime: '14:00:00',
    daysOfWeek: [],
  };
  assert.equal(resolvePromotionStatus(inactivePromo, fridayNoon), PROMOTION_STATUSES.INACTIVE);

  const runningEveryDay = {
    active: true,
    startTime: '11:00:00',
    endTime: '14:00:00',
    daysOfWeek: [],
  };
  assert.equal(resolvePromotionStatus(runningEveryDay, fridayNoon), PROMOTION_STATUSES.RUNNING);
  assert.equal(resolvePromotionStatus(runningEveryDay, fridayEvening), PROMOTION_STATUSES.SCHEDULED);

  const weekendOnly = {
    active: true,
    startTime: '11:00:00',
    endTime: '14:00:00',
    daysOfWeek: ['SATURDAY', 'SUNDAY'],
  };
  assert.equal(resolvePromotionStatus(weekendOnly, fridayNoon), PROMOTION_STATUSES.SCHEDULED);

  const fridayRunning = {
    active: true,
    startTime: '11:00:00',
    endTime: '14:00:00',
    daysOfWeek: ['FRIDAY'],
  };
  assert.equal(resolvePromotionStatus(fridayRunning, fridayNoon), PROMOTION_STATUSES.RUNNING);
});

test('errorMessages translates promotion error messages and codes', () => {
  assert.equal(
    translateErrorMessage('Promotion name already exists', 'PROMOTION_NAME_EXISTS'),
    'Tên chương trình khuyến mãi đã tồn tại.',
  );
  assert.equal(
    translateErrorMessage('End time must be after start time', 'VALIDATION_ERROR'),
    'Giờ kết thúc phải sau giờ bắt đầu.',
  );
});
