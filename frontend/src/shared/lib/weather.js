/**
 * Weather code → Vietnamese label, enriched with temperature context.
 */
export const getWeatherLabel = (code, temperature) => {
  const hot = temperature >= 35;
  const warm = temperature >= 28;
  const cool = temperature <= 22;

  if (code === 0 || [1, 2].includes(code)) {
    if (hot) return 'Nắng gắt';
    if (warm) return 'Nắng ấm';
    if (cool) return 'Se se lạnh';
    return 'Trời quang';
  }

  if (code === 3) {
    if (hot) return 'Oi bức';
    if (warm) return 'Mát mẻ';
    if (cool) return 'Lạnh';
    return 'Nhiều mây';
  }

  if ([45, 48].includes(code)) {
    if (cool) return 'Sương mù lạnh';
    return 'Có sương mù';
  }

  if (code >= 51 && code <= 57) {
    if (hot) return 'Mưa phùn nóng';
    return 'Mưa phùn';
  }

  if (code >= 61 && code <= 67) {
    if (hot) return 'Mưa nóng';
    if (warm) return 'Mưa ấm';
    return 'Có mưa';
  }

  if (code >= 80 && code <= 82) return 'Mưa rào';

  if ([95, 96, 99].includes(code)) {
    if (hot) return 'Dông nhiệt';
    return 'Có dông';
  }

  return '';
};

/**
 * Map weather code + isDay → emoji (context-aware).
 */
export const getWeatherEmoji = (code, isDay) => {
  if (code === 0) return isDay ? '☀️' : '🌙';
  if ([1, 2].includes(code)) return isDay ? '🌤️' : '🌙';
  if (code === 3) return '☁️';
  if ([45, 48].includes(code)) return '🌫️';
  if (code >= 51 && code <= 57) return '🌦️';
  if (code >= 61 && code <= 67) return '🌧️';
  if (code >= 80 && code <= 82) return '🌧️';
  if ([95, 96, 99].includes(code)) return '⛈️';
  return isDay ? '☀️' : '🌙';
};

/**
 * Compose a short weather line for display.
 * e.g. "☀️ 39°C · Nắng gắt" or "☁️ 39°C · Oi bức"
 */
export const formatWeather = (weather) => {
  if (!weather) return null;

  const { temperature, weatherCode, isDay } = weather;
  const emoji = getWeatherEmoji(weatherCode, isDay);
  const label = getWeatherLabel(weatherCode, temperature);

  return `${emoji} ${temperature}°C${label ? ` · ${label}` : ''}`;
};
