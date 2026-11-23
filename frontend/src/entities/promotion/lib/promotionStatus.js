export const PROMOTION_STATUSES = Object.freeze({
  RUNNING: 'RUNNING',
  SCHEDULED: 'SCHEDULED',
  INACTIVE: 'INACTIVE',
});

export const DAYS_OF_WEEK_ORDER = Object.freeze([
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]);

export const WEEKDAY_CODES = Object.freeze([
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
]);

export const WEEKEND_CODES = Object.freeze(['SATURDAY', 'SUNDAY']);

const JS_DAY_TO_ENUM = Object.freeze([
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]);

/**
 * Parses a promotion time string ("HH:mm" or "HH:mm:ss") into minutes since midnight.
 */
export const parseTimeToMinutes = (timeValue) => {
  if (typeof timeValue !== 'string') return null;
  const trimmed = timeValue.trim();
  const match = /^([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?$/.exec(trimmed);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  return hours * 60 + minutes;
};

/**
 * Formats an API LocalTime string ("HH:mm:ss" or "HH:mm") into "HH:mm" for display and inputs.
 */
export const formatPromotionTime = (timeValue) => {
  if (typeof timeValue !== 'string' || !timeValue.trim()) return '';
  const match = /^([01]\d|2[0-3]):([0-5]\d)/.exec(timeValue.trim());
  if (!match) return '';
  return `${match[1]}:${match[2]}`;
};

/**
 * Normalizes a time string ("HH:mm" or "HH:mm:ss") for Spring LocalTime deserialization.
 */
export const toApiLocalTime = (timeValue) => {
  if (typeof timeValue !== 'string' || !timeValue.trim()) return '';
  const trimmed = timeValue.trim();
  if (/^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/.test(trimmed)) return trimmed;
  const formatted = formatPromotionTime(trimmed);
  return formatted ? `${formatted}:00` : '';
};

/**
 * Returns the Java DayOfWeek enum name for a given Date.
 */
export const getCurrentDayOfWeek = (date = new Date()) => JS_DAY_TO_ENUM[date.getDay()];

/**
 * Filters and sorts DayOfWeek strings in Monday..Sunday display order.
 */
export const sortDaysOfWeek = (days = []) => {
  const unique = new Set(Array.isArray(days) ? days : Array.from(days || []));
  return DAYS_OF_WEEK_ORDER.filter((day) => unique.has(day));
};

/**
 * Checks whether a promotion applies on every day of the week (empty list or all 7 days).
 */
export const isEveryDayPromotion = (days = []) => {
  const sorted = sortDaysOfWeek(days);
  return sorted.length === 0 || sorted.length === DAYS_OF_WEEK_ORDER.length;
};

/**
 * Resolves the live operational status of a promotion for a given point in time.
 */
export const resolvePromotionStatus = (promotion, now = new Date()) => {
  if (!promotion || !promotion.active) {
    return PROMOTION_STATUSES.INACTIVE;
  }

  const days = sortDaysOfWeek(promotion.daysOfWeek);
  const dayMatches = days.length === 0 || days.includes(getCurrentDayOfWeek(now));

  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const startMinutes = parseTimeToMinutes(promotion.startTime);
  const endMinutes = parseTimeToMinutes(promotion.endTime);
  const timeMatches =
    startMinutes !== null &&
    endMinutes !== null &&
    nowMinutes >= startMinutes &&
    nowMinutes <= endMinutes;

  return dayMatches && timeMatches
    ? PROMOTION_STATUSES.RUNNING
    : PROMOTION_STATUSES.SCHEDULED;
};
