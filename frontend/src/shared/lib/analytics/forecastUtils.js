export const getForecastSummary = (revenueForecast = []) => {
  const actualPoints = revenueForecast.filter((item) => !item.forecasted);
  const forecastPoints = revenueForecast.filter((item) => item.forecasted);

  const recentActualPoints = actualPoints.slice(-7);

  const avgActual7d = recentActualPoints.length
    ? recentActualPoints.reduce((sum, item) => sum + Number(item.revenue || item.actual || 0), 0) /
      recentActualPoints.length
    : 0;

  // Lấy dự báo của ngày mai (phần tử dự báo đầu tiên)
  const forecastTomorrow = Number(forecastPoints[0]?.forecast || forecastPoints[0]?.revenue || 0);

  const diffPercent = avgActual7d > 0
    ? ((forecastTomorrow - avgActual7d) / avgActual7d) * 100
    : 0;

  const trend =
    diffPercent >= 10
      ? 'UP'
      : diffPercent <= -10
        ? 'DOWN'
        : 'STABLE';

  const confidence =
    actualPoints.length >= 28
      ? 'MEDIUM'
      : actualPoints.length >= 14
        ? 'LOW'
        : 'VERY_LOW';

  return {
    forecastTomorrow,
    avgActual7d,
    diffPercent,
    trend,
    confidence,
  };
};

export const getTrendLabel = (trend, language = 'vi') => {
  const labels = {
    UP: { vi: 'Đang tăng', en: 'Upward' },
    DOWN: { vi: 'Đang giảm', en: 'Declining' },
    STABLE: { vi: 'Ổn định', en: 'Stable' },
  };
  return labels[trend]?.[language] ?? trend;
};

export const getRevenueInsight = (summary, language = 'vi') => {
  if (!summary.avgActual7d || !summary.forecastTomorrow) {
    return language === 'en'
      ? 'Not enough data to provide a revenue insight.'
      : 'Chưa đủ dữ liệu để đưa ra nhận xét doanh thu.';
  }

  const percent = Math.round(summary.diffPercent);

  if (percent >= 15) {
    return language === 'en'
      ? `Tomorrow's revenue is forecast to be ~${percent}% above the 7-day average. Consider staffing up and preparing more ingredients for peak hours.`
      : `Doanh thu ngày mai được dự báo cao hơn trung bình 7 ngày khoảng ${percent}%. Nên chuẩn bị nhân sự và nguyên liệu tốt hơn cho khung giờ cao điểm.`;
  }

  if (percent <= -15) {
    return language === 'en'
      ? `Tomorrow's revenue is forecast to be ~${Math.abs(percent)}% below the 7-day average. Consider running a promotion or reviewing reservations.`
      : `Doanh thu ngày mai được dự báo thấp hơn trung bình 7 ngày khoảng ${Math.abs(percent)}%. Có thể cân nhắc chương trình khuyến mãi hoặc kiểm tra lịch đặt bàn.`;
  }

  return language === 'en'
    ? 'Forecast revenue is relatively stable compared to the recent 7-day average.'
    : 'Doanh thu dự báo đang tương đối ổn định so với trung bình 7 ngày gần đây.';
};

export const getDemandLevel = (quantity, language = 'vi') => {
  if (quantity >= 25) {
    return {
      label: language === 'en' ? 'High' : 'Cao',
      className: 'bg-red-50 text-red-600 dark:bg-red-950/30 dark:text-red-300',
      color: 'red',
    };
  }

  if (quantity >= 12) {
    return {
      label: language === 'en' ? 'Medium' : 'Trung bình',
      className: 'bg-orange-50 text-orange-600 dark:bg-orange-950/30 dark:text-orange-300',
      color: 'orange',
    };
  }

  return {
    label: language === 'en' ? 'Low' : 'Thấp',
    className: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300',
    color: 'slate',
  };
};
