export const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh';

const businessDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

export const formatBusinessDate = (value = new Date()) => {
  if (typeof value === 'string') {
    return value;
  }

  if (!(value instanceof Date) || Number.isNaN(value.getTime())) {
    throw new TypeError('Expected a valid Date or ISO date string');
  }

  const parts = Object.fromEntries(
    businessDateFormatter
      .formatToParts(value)
      .filter(({ type }) => type !== 'literal')
      .map(({ type, value: partValue }) => [type, partValue]),
  );

  return `${parts.year}-${parts.month}-${parts.day}`;
};

export const addDaysToBusinessDate = (date, days) => {
  const [year, month, day] = formatBusinessDate(date).split('-').map(Number);
  const result = new Date(Date.UTC(year, month - 1, day + days));

  return [
    result.getUTCFullYear(),
    String(result.getUTCMonth() + 1).padStart(2, '0'),
    String(result.getUTCDate()).padStart(2, '0'),
  ].join('-');
};

export const getBusinessToday = () => formatBusinessDate(new Date());
