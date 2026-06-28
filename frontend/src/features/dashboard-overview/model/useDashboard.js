import { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { dashboardOverviewService } from '../api/dashboardOverviewService.js';
import { getBusinessToday } from '@shared/lib/businessTime.js';
import { queryKeys } from '@shared/api/queryKeys.js';

const toNumber = (value) => Number(value || 0);

const buildCategoryShares = (dishes) => {
  const totalsByCategory = dishes.reduce((acc, dish) => {
    const category = dish.category || 'Khác';
    acc[category] = (acc[category] || 0) + toNumber(dish.totalRevenue);
    return acc;
  }, {});
  return Object.entries(totalsByCategory).map(([name, value]) => ({ name, value }));
};

const buildRevenueForecastData = (points) => {
  const rows = points.map((point) => ({
    date: point.date,
    label: formatChartDate(point.date),
    actual: point.actual == null ? null : Number(point.actual),
    forecast: point.forecast == null ? null : Number(point.forecast),
    forecasted: point.forecasted,
  }));

  const firstForecastIndex = rows.findIndex((row) => row.forecasted);
  if (firstForecastIndex > 0 && rows[firstForecastIndex - 1].actual != null) {
    rows[firstForecastIndex - 1] = {
      ...rows[firstForecastIndex - 1],
      forecast: rows[firstForecastIndex - 1].actual,
    };
  }

  return rows;
};

const formatChartDate = (date) => {
  if (!date) return '';
  const [, month, day] = date.split('-');
  return `${day}/${month}`;
};

const useDashboard = () => {
  const [businessToday, setBusinessToday] = useState(getBusinessToday());

  useEffect(() => {
    const syncBusinessDate = () => {
      setBusinessToday((currentDate) => {
        const nextDate = getBusinessToday();
        return currentDate === nextDate ? currentDate : nextDate;
      });
    };

    const intervalId = window.setInterval(syncBusinessDate, 30_000);
    document.addEventListener('visibilitychange', syncBusinessDate);

    return () => {
      window.clearInterval(intervalId);
      document.removeEventListener('visibilitychange', syncBusinessDate);
    };
  }, []);

  const { data: dashboard, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.analytics.dashboard({ businessDate: businessToday }),
    queryFn: ({ signal }) => dashboardOverviewService.getSummary(businessToday, { signal }),
    staleTime: 10_000,
  });

  const tables = dashboard?.tables || {};
  const todayOrderSummary = dashboard?.todayOrders || {};
  const dishes = useMemo(
    () => (Array.isArray(dashboard?.topDishes) ? dashboard.topDishes : []),
    [dashboard],
  );

  // todayRevenue và todayAvgOrderValue: backend tính riêng cho ngày hôm nay
  const todayRevenue = toNumber(dashboard?.todayRevenue);
  const todayOrders = {
    total: toNumber(todayOrderSummary.total),
    completed: toNumber(todayOrderSummary.completed),
  };
  const tablesContext = {
    total: toNumber(tables.total),
    occupied: toNumber(tables.occupied),
  };

  // avgOrderValue: lấy todayAvgOrderValue từ backend (cùng tập dữ liệu hôm nay)
  const avgOrderValue = toNumber(dashboard?.todayAvgOrderValue);

  const topDishes = useMemo(() => dishes.slice(0, 5), [dishes]);
  const categoryShares = useMemo(() => buildCategoryShares(dishes), [dishes]);
  const recentOrders = Array.isArray(dashboard?.recentOrders) ? dashboard.recentOrders : [];

  const revenueForecastData = useMemo(
    () =>
      buildRevenueForecastData(
        Array.isArray(dashboard?.revenueForecast) ? dashboard.revenueForecast : [],
      ),
    [dashboard],
  );

  const popularDishesForecast = Array.isArray(dashboard?.popularDishesForecast)
    ? dashboard.popularDishesForecast
    : [];

  return {
    loading: isLoading,
    error,
    refetch,
    todayRevenue,
    todayOrders,
    avgOrderValue,
    tablesContext,
    topDishes,
    categoryShares,
    recentOrders,
    revenueForecastData,
    popularDishesForecast,
  };
};

export default useDashboard;
