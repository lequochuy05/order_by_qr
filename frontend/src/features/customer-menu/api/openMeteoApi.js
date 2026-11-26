const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

/**
 * Fetch current weather from Open-Meteo (free, no API key required).
 *
 * @param {{ lat: number, lon: number }} coords
 * @returns {Promise<{ temperature: number, apparentTemperature: number, weatherCode: number, isDay: number, precipitation: number, windSpeed: number }>}
 */
export const fetchWeather = async ({ lat, lon }) => {
  if (lat == null || lon == null) {
    throw new Error('Missing restaurant coordinates');
  }

  const params = new URLSearchParams({
    latitude: String(lat),
    longitude: String(lon),
    current: [
      'temperature_2m',
      'apparent_temperature',
      'weather_code',
      'is_day',
      'precipitation',
      'wind_speed_10m',
    ].join(','),
    timezone: 'auto',
  });

  const response = await fetch(`${BASE_URL}?${params}`);

  if (!response.ok) {
    throw new Error(`Open-Meteo API error: ${response.status}`);
  }

  const data = await response.json();
  const current = data.current;

  return {
    temperature: Math.round(current.temperature_2m),
    apparentTemperature: Math.round(current.apparent_temperature),
    weatherCode: current.weather_code,
    isDay: current.is_day,
    precipitation: current.precipitation,
    windSpeed: current.wind_speed_10m,
  };
};
