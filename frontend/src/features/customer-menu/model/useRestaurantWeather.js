import { useQuery } from '@tanstack/react-query';
import { fetchWeather } from '@shared/api/openMeteoApi.js';
import { formatWeather } from '@shared/lib/weather.js';

const getCoords = () => ({
  lat: import.meta.env.VITE_RESTAURANT_LAT
    ? Number(import.meta.env.VITE_RESTAURANT_LAT)
    : null,
  lon: import.meta.env.VITE_RESTAURANT_LON
    ? Number(import.meta.env.VITE_RESTAURANT_LON)
    : null,
});

/**
 * Fetch restaurant weather via Open-Meteo.
 *
 * Returns { weather, weatherLine, isLoading, isError }
 * - weather: raw API response or null
 * - weatherLine: formatted display string or null
 * - isLoading: true during fetch
 * - isError: true on failure or missing coords
 *
 * Never throws — always returns a safe shape.
 * Does NOT block rendering when loading/error.
 */
const useRestaurantWeather = () => {
  const { lat, lon } = getCoords();
  const hasCoords = lat != null && lon != null && !Number.isNaN(lat) && !Number.isNaN(lon);

  const query = useQuery({
    queryKey: ['restaurantWeather', lat, lon],
    queryFn: () => fetchWeather({ lat, lon }),
    enabled: hasCoords,
    staleTime: 30 * 60 * 1000, // 30 minutes — weather changes slowly
    retry: 1,
    refetchOnWindowFocus: false,
  });

  const weatherLine = query.data ? formatWeather(query.data) : null;
  const isError = query.isError || !hasCoords;
  const isLoading = query.isLoading && hasCoords;

  return {
    weather: query.data ?? null,
    weatherLine,
    isLoading,
    isError,
  };
};

export default useRestaurantWeather;
