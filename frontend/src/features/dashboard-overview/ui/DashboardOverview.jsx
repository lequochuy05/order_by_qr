import React, { useState } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
} from 'recharts';
import {
  TrendingUp,
  ShoppingBag,
  UtensilsCrossed,
  Clock,
  Loader2,
  Sparkles,
  Banknote,
  AlertCircle,
  RefreshCw,
  ArrowUpRight,
  ArrowDownRight,
  CheckCircle2,
  Lightbulb,
  ChefHat,
  Receipt,
  Layers,
  Activity,
} from 'lucide-react';
import { fmtVND, fmtTime, fmtCompactVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import {
  getForecastSummary,
  getTrendLabel,
  getDemandLevel,
  getRevenueInsight,
} from '@shared/lib/analytics/forecastUtils.js';
import useDashboard from '../model/useDashboard.js';

// Modern, vibrant Evondev-style color palette for donut chart
const PIE_COLORS = [
  '#6366F1', // Indigo
  '#3B82F6', // Blue
  '#10B981', // Emerald
  '#F59E0B', // Amber
  '#EC4899', // Pink
  '#8B5CF6', // Purple
  '#14B8A6', // Teal
  '#F97316', // Orange
];

// Custom Tooltip for Forecast Area/Line Chart
const CustomForecastTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="rounded-2xl border border-[#1E293B] bg-white/95 p-3.5 shadow-xl backdrop-blur-md dark:border-[#1E293B] dark:bg-slate-900/95">
        <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 mb-2">
          {data.date || label}
        </p>
        <div className="space-y-1.5">
          {payload.map((entry, index) => {
            if (entry.value == null) return null;
            const isActual = entry.dataKey === 'actual';
            return (
              <div
                key={`tooltip-${index}`}
                className="flex items-center justify-between gap-4 text-xs"
              >
                <span className="flex items-center gap-1.5 font-medium text-slate-600 dark:text-slate-300">
                  <span
                    className="w-2.5 h-2.5 rounded-full"
                    style={{ backgroundColor: isActual ? '#6366F1' : '#F59E0B' }}
                  />
                  {isActual ? 'Doanh thu thực tế' : 'Dự báo AI'}:
                </span>
                <span className="font-bold text-slate-900 dark:text-slate-100 font-mono">
                  {fmtVND(entry.value)}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    );
  }
  return null;
};

// Custom Tooltip for Category Donut Chart
const CustomPieTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0];
    return (
      <div className="rounded-2xl border border-[#1E293B] bg-white/95 px-3.5 py-2.5 shadow-xl backdrop-blur-md dark:border-[#1E293B] dark:bg-slate-900/95">
        <p className="text-xs font-bold text-slate-800 dark:text-slate-200 mb-1">{data.name}</p>
        <p className="text-sm font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">
          {fmtVND(data.value)}
        </p>
      </div>
    );
  }
  return null;
};

const DashboardOverview = () => {
  const {
    loading,
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
  } = useDashboard();

  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleManualRefresh = async () => {
    try {
      setIsRefreshing(true);
      await refetch();
    } finally {
      setIsRefreshing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[480px] flex-col items-center justify-center gap-3">
        <div className="relative flex items-center justify-center">
          <div className="w-14 h-14 rounded-full border-4 border-indigo-100 dark:border-indigo-950/60 border-t-indigo-600 dark:border-t-indigo-500 animate-spin" />
          <Activity className="absolute text-indigo-600 dark:text-indigo-400 w-6 h-6 animate-pulse" />
        </div>
        <p className="text-sm font-semibold text-slate-500 dark:text-slate-400">
          Đang cập nhật số liệu kinh doanh...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[480px] items-center justify-center p-6">
        <div className="rounded-3xl border border-[#1E293B] bg-white/95 dark:bg-[#0F172A]/95 p-8 text-center max-w-md shadow-xl">
          <div className="w-14 h-14 rounded-2xl bg-rose-50 dark:bg-rose-950/40 text-rose-500 flex items-center justify-center mx-auto mb-4">
            <AlertCircle size={30} />
          </div>
          <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-1">
            Không thể tải dữ liệu
          </h3>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-6">
            Đã xảy ra lỗi khi kết nối với máy chủ. Vui lòng kiểm tra lại đường truyền mạng.
          </p>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-5 py-2.5 text-sm font-bold text-white hover:from-rose-700 hover:to-red-700 transition-all shadow-md shadow-rose-600/25 active:scale-95"
          >
            <RefreshCw size={16} />
            Thử lại ngay
          </button>
        </div>
      </div>
    );
  }

  const forecastSummary = getForecastSummary(revenueForecastData);
  const revenueInsight = getRevenueInsight(forecastSummary);

  // Calculate table occupancy percentage
  const occupancyPercent =
    tablesContext.total > 0
      ? Math.round((tablesContext.occupied / tablesContext.total) * 100)
      : 0;

  // Max revenue among top dishes to calculate proportional width
  const maxTopDishRevenue = topDishes[0]?.totalRevenue || 1;

  // Total category revenue to calculate percentages
  const totalCategoryRevenue = categoryShares.reduce((acc, item) => acc + (item.value || 0), 0);

  return (
    <div className="w-full min-w-0 space-y-6 lg:space-y-8 animate-fade-in-up">
      {/* =========================================================================
          TOP BANNER / HEADER: Welcome & Live Status
            ========================================================================= */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-[#1E293B]/20 dark:border-[#1E293B]">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
                Tổng quan kinh doanh
              </h1>
              <span className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-[#1E293B]/25 dark:border-[#1E293B]">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
                Trực tiếp
              </span>
            </div>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Theo dõi doanh thu, hiệu suất vận hành bàn và dự báo kinh doanh thông minh.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleManualRefresh}
              disabled={isRefreshing}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white dark:bg-[#0F172A] border border-[#1E293B] text-xs font-bold text-slate-800 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 shadow-sm transition-all active:scale-95 disabled:opacity-60"
            >
              <RefreshCw size={14} className={isRefreshing ? 'animate-spin text-indigo-600' : ''} />
              <span>{isRefreshing ? 'Đang cập nhật...' : 'Làm mới số liệu'}</span>
            </button>
          </div>
        </div>

        {/* =========================================================================
            1. 4 STATS KPI CARDS: Modern Sleek Cards with #1E293B Dark Borders
            ========================================================================= */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {/* KPI 1: Doanh thu hôm nay */}
          <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Doanh thu hôm nay
                </p>
                <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                  {fmtVND(todayRevenue)}
                </h3>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:scale-110 transition-transform duration-300">
                <TrendingUp size={22} />
              </div>
            </div>
            <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
              <span className="inline-flex items-center gap-1 font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded-md">
                <ArrowUpRight size={12} />
                Theo ca bán
              </span>
              <span>Doanh thu tính từ 00:00</span>
            </div>
          </div>

          {/* KPI 2: Đơn hàng hôm nay */}
          <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Đơn hàng hôm nay
                </p>
                <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                  {todayOrders.total}
                </h3>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-cyan-500 text-white flex items-center justify-center shadow-lg shadow-blue-500/25 group-hover:scale-110 transition-transform duration-300">
                <ShoppingBag size={22} />
              </div>
            </div>
            <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
              <span className="inline-flex items-center gap-1 font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded-md">
                <CheckCircle2 size={12} />
                {todayOrders.completed} hoàn thành
              </span>
              <span>{todayOrders.total - todayOrders.completed} đang xử lý</span>
            </div>
          </div>

          {/* KPI 3: Bàn đang hoạt động */}
          <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Bàn đang phục vụ
                </p>
                <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
                  {tablesContext.occupied}
                  <span className="text-lg font-bold text-slate-400 dark:text-slate-500 ml-1">
                    / {tablesContext.total}
                  </span>
                </h3>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/25 group-hover:scale-110 transition-transform duration-300">
                <UtensilsCrossed size={22} />
              </div>
            </div>
            <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
              <span className="inline-flex items-center gap-1 font-bold text-teal-600 dark:text-teal-400 bg-teal-50 dark:bg-teal-950/40 px-2 py-0.5 rounded-md">
                {occupancyPercent}% công suất
              </span>
              <span>Đang có khách ngồi</span>
            </div>
          </div>

          {/* KPI 4: Giá trị TB / đơn */}
          <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Giá trị TB / đơn
                </p>
                <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                  {fmtVND(avgOrderValue)}
                </h3>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-500 text-white flex items-center justify-center shadow-lg shadow-amber-500/25 group-hover:scale-110 transition-transform duration-300">
              <Banknote size={22} />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800/60 text-xs text-slate-500 dark:text-slate-400">
            <span className="inline-flex items-center gap-1 font-bold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2 py-0.5 rounded-md">
              AOV
            </span>
            <span>Hiệu quả chi tiêu khách hàng</span>
          </div>
        </div>
      </div>

      {/* =========================================================================
          2. AI FORECASTING & SMART KITCHEN PREP (2/3 + 1/3)
          ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: AI Revenue Forecast Chart */}
        <div className="min-w-0 lg:col-span-2 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div>
              <div className="flex items-center gap-2">
                <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
                  <TrendingUp size={20} />
                </span>
                <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
                  Dự báo doanh thu 7 ngày tới
                </h3>
              </div>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Kết hợp dữ liệu bán hàng thực tế và mô hình AI dự báo chu kỳ
              </p>
            </div>

            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 text-indigo-600 dark:text-indigo-400 border border-[#1E293B]/25 dark:border-[#1E293B] shadow-sm self-start sm:self-auto">
              <Sparkles size={14} className="animate-spin text-indigo-500" />
              <span>AI Insights</span>
            </div>
          </div>

          {/* Forecast Metric Mini-cards */}
          {revenueForecastData.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
              <div className="rounded-2xl bg-indigo-50/60 dark:bg-indigo-950/30 border border-[#1E293B]/25 dark:border-[#1E293B] p-4">
                <p className="text-xs font-bold uppercase tracking-wider text-indigo-900/70 dark:text-indigo-300">
                  Dự báo ngày mai
                </p>
                <p className="mt-1 text-lg font-black text-indigo-600 dark:text-indigo-400 font-mono">
                  {fmtVND(forecastSummary.forecastTomorrow)}
                </p>
              </div>

              <div className="rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-[#1E293B]/25 dark:border-[#1E293B] p-4">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  So với TB 7 ngày
                </p>
                <p className="mt-1 text-lg font-black text-slate-900 dark:text-white font-mono flex items-center gap-1">
                  {forecastSummary.diffPercent >= 0 ? (
                    <ArrowUpRight size={18} className="text-emerald-500" />
                  ) : (
                    <ArrowDownRight size={18} className="text-rose-500" />
                  )}
                  {forecastSummary.diffPercent >= 0 ? '+' : ''}
                  {Math.round(forecastSummary.diffPercent)}%
                </p>
              </div>

              <div className="rounded-2xl bg-emerald-50/60 dark:bg-emerald-950/30 border border-[#1E293B]/25 dark:border-[#1E293B] p-4">
                <p className="text-xs font-bold uppercase tracking-wider text-emerald-900/70 dark:text-emerald-300">
                  Xu hướng
                </p>
                <p className="mt-1 text-lg font-black text-emerald-600 dark:text-emerald-400 flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                  {getTrendLabel(forecastSummary.trend)}
                </p>
              </div>
            </div>
          )}

          {/* AI Guidance Box */}
          {revenueForecastData.length > 0 && revenueInsight && (
            <div className="mb-6 flex items-start gap-3 rounded-2xl bg-gradient-to-r from-indigo-50/80 via-purple-50/50 to-pink-50/30 dark:from-indigo-950/25 dark:via-purple-950/20 dark:to-pink-950/10 border border-[#1E293B]/25 dark:border-[#1E293B] p-4 text-xs font-medium text-slate-700 dark:text-slate-300">
              <Lightbulb className="w-5 h-5 text-indigo-600 dark:text-indigo-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold text-indigo-900 dark:text-indigo-300">
                  Gợi ý điều hành:{' '}
                </span>
                <span>{revenueInsight}</span>
              </div>
            </div>
          )}

          {/* Legend */}
          <div className="flex items-center gap-6 text-xs font-semibold text-slate-500 dark:text-slate-400 mb-4">
            <span className="flex items-center gap-2">
              <span className="w-3.5 h-3.5 rounded-md bg-indigo-500" />
              Doanh thu thực tế
            </span>
            <span className="flex items-center gap-2">
              <span className="w-3.5 h-1 border-t-2 border-dashed border-amber-500" />
              Dự báo bởi AI
            </span>
          </div>

          {/* Chart Container */}
          <div className="h-72 w-full">
            {revenueForecastData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={revenueForecastData}
                  margin={{ top: 10, right: 10, bottom: 5, left: 0 }}
                >
                  <defs>
                    <linearGradient id="actualGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366F1" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#6366F1" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#E2E8F0"
                    opacity={0.5}
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 11, fill: '#64748B' }}
                    axisLine={false}
                    tickLine={false}
                    dy={5}
                  />
                  <YAxis
                    tickFormatter={fmtCompactVND}
                    tick={{ fontSize: 11, fill: '#64748B' }}
                    axisLine={false}
                    tickLine={false}
                    width={50}
                  />
                  <Tooltip content={<CustomForecastTooltip />} />
                  <Area
                    type="monotone"
                    dataKey="actual"
                    stroke="#6366F1"
                    strokeWidth={3}
                    fill="url(#actualGrad)"
                    activeDot={{ r: 6, fill: '#6366F1', stroke: '#fff', strokeWidth: 2 }}
                    connectNulls={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="forecast"
                    stroke="#F59E0B"
                    strokeWidth={2.5}
                    strokeDasharray="5 5"
                    dot={{ r: 4, fill: '#F59E0B', stroke: '#fff', strokeWidth: 1.5 }}
                    activeDot={{ r: 6, fill: '#F59E0B' }}
                    connectNulls={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-400">
                Chưa có dữ liệu dự báo doanh thu
              </div>
            )}
          </div>
        </div>

        {/* Right: AI Prep Recommendations for Kitchen */}
        <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <span className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
                  <ChefHat size={20} />
                </span>
                <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
                  Gợi ý chuẩn bị món
                </h3>
              </div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                30 Ngày qua
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-5">
              Dự kiến số lượng suất ăn cần chuẩn bị trước để tối ưu thời gian ra món.
            </p>

            <div className="space-y-3.5">
              {popularDishesForecast.map((dish, idx) => {
                const demand = getDemandLevel(dish.estimatedQty);
                const rankColors = [
                  'bg-gradient-to-r from-amber-400 to-amber-600 text-white shadow-amber-500/20',
                  'bg-gradient-to-r from-slate-400 to-slate-600 text-white shadow-slate-500/20',
                  'bg-gradient-to-r from-amber-700 to-amber-900 text-white shadow-amber-800/20',
                ];
                const badgeColor =
                  rankColors[idx] ||
                  'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300';

                return (
                  <div
                    key={dish.id || idx}
                    className="group rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/40 p-3.5 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div
                          className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-black shadow-sm shrink-0 ${badgeColor}`}
                        >
                          {idx + 1}
                        </div>
                        <div className="min-w-0">
                          <p
                            className="font-bold text-sm text-slate-900 dark:text-white truncate"
                            title={dish.name}
                          >
                            {dish.name}
                          </p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            Dự kiến bán:{' '}
                            <span className="font-extrabold text-indigo-600 dark:text-indigo-400">
                              {dish.estimatedQty}
                            </span>{' '}
                            suất
                          </p>
                        </div>
                      </div>

                      <span
                        className={`shrink-0 rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${demand.className}`}
                      >
                        {demand.label}
                      </span>
                    </div>

                    {/* Progress Bar */}
                    <div className="mt-3 h-1.5 w-full rounded-full bg-slate-200/70 dark:bg-slate-700 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-500"
                        style={{
                          width: `${Math.min(100, (dish.estimatedQty / 30) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>
                );
              })}

              {popularDishesForecast.length === 0 && (
                <div className="py-8 text-center text-slate-400 text-xs">
                  Chưa có dữ liệu dự báo món ăn hôm nay.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* =========================================================================
          3. BOTTOM 3 COLUMNS: Category Shares, Top 5 Dishes & Recent Transactions
          ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Col 1: Tỉ trọng danh mục (Donut Chart) */}
        <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400">
                <Layers size={20} />
              </span>
              <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
                Tỉ trọng danh mục
              </h3>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
              Doanh thu phân bổ theo các nhóm món ăn trong 7 ngày
            </p>

            <div className="h-56 relative flex items-center justify-center">
              {categoryShares.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryShares}
                      cx="50%"
                      cy="50%"
                      innerRadius={62}
                      outerRadius={82}
                      paddingAngle={4}
                      cornerRadius={6}
                      dataKey="value"
                    >
                      {categoryShares.map((entry, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={PIE_COLORS[index % PIE_COLORS.length]}
                          stroke="transparent"
                        />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomPieTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="text-center text-slate-400 text-xs">Chưa có thông tin bán hàng</div>
              )}

              {/* Center Donut Label */}
              {categoryShares.length > 0 && (
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Tổng cộng
                  </span>
                  <span className="text-xs font-black text-slate-800 dark:text-slate-200 font-mono">
                    {fmtCompactVND(totalCategoryRevenue)}
                  </span>
                </div>
              )}
            </div>

            {/* Custom Category Legend Badges */}
            <div className="mt-4 flex flex-wrap gap-2">
              {categoryShares.map((entry, index) => {
                const percent =
                  totalCategoryRevenue > 0
                    ? Math.round((entry.value / totalCategoryRevenue) * 100)
                    : 0;
                const color = PIE_COLORS[index % PIE_COLORS.length];
                return (
                  <div
                    key={`legend-${index}`}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-50/80 dark:bg-slate-800/60 border border-[#1E293B]/20 dark:border-[#1E293B] text-xs text-slate-700 dark:text-slate-300"
                  >
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ backgroundColor: color }}
                    />
                    <span className="font-medium truncate max-w-[90px]">{entry.name}</span>
                    <span className="font-bold text-slate-900 dark:text-white font-mono">
                      {percent}%
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Col 2: Top 5 Món bán chạy (7 ngày) */}
        <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
          <div className="flex items-center gap-2 mb-1">
            <span className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400">
              <UtensilsCrossed size={20} />
            </span>
            <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
              Top món bán chạy
            </h3>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
            5 món có số lượng và doanh thu cao nhất 7 ngày qua
          </p>

          <div className="space-y-3.5">
            {topDishes.map((dish, idx) => {
              const rankStyles = [
                'bg-gradient-to-tr from-amber-400 to-amber-600 text-white shadow-amber-500/25',
                'bg-gradient-to-tr from-slate-400 to-slate-600 text-white shadow-slate-500/25',
                'bg-gradient-to-tr from-amber-700 to-amber-900 text-white shadow-amber-800/25',
              ];
              const rankClass =
                rankStyles[idx] ||
                'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300';

              const barPercent = Math.min(
                100,
                Math.round((dish.totalRevenue / maxTopDishRevenue) * 100),
              );

              return (
                <div
                  key={idx}
                  className="rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/30 p-3.5 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div
                        className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-black shadow-sm shrink-0 ${rankClass}`}
                      >
                        {idx + 1}
                      </div>
                      <div className="min-w-0">
                        <p
                          className="font-bold text-sm text-slate-900 dark:text-white truncate"
                          title={dish.name}
                        >
                          {dish.name}
                        </p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          Đã bán:{' '}
                          <span className="font-extrabold text-slate-800 dark:text-slate-200">
                            {dish.totalQty}
                          </span>{' '}
                          phần
                        </p>
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="text-sm font-black text-indigo-600 dark:text-indigo-400 font-mono">
                        {fmtVND(dish.totalRevenue)}
                      </p>
                    </div>
                  </div>

                  {/* Volume Ratio Bar */}
                  <div className="mt-2.5 h-1 w-full rounded-full bg-slate-200/60 dark:bg-slate-700/60 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-indigo-500/80"
                      style={{ width: `${barPercent}%` }}
                    />
                  </div>
                </div>
              );
            })}

            {topDishes.length === 0 && (
              <div className="py-8 text-center text-slate-400 text-xs">
                Chưa có món nào được ghi nhận bán ra.
              </div>
            )}
          </div>
        </div>

        {/* Col 3: Đơn hàng mới nhất (Recent Transactions Stream) */}
        <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
          <div className="flex items-center gap-2 mb-1">
            <span className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
              <Clock size={20} />
            </span>
            <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
              Đơn hàng mới nhất
            </h3>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
            Nhật ký giao dịch phát sinh gần nhất trong ngày
          </p>

          <div className="space-y-3">
            {recentOrders.map((order, idx) => {
              const tableNumber = order.table?.tableNumber || order.tableNumber;
              const orderAmount = order.finalAmount ?? getOrderFinalAmount(order);
              const statusMeta = getOrderStatusMeta(order.status);

              return (
                <div
                  key={order.id || idx}
                  className="flex items-center justify-between p-3 rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/30 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 flex items-center justify-center shrink-0">
                      <Receipt size={18} />
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900 dark:text-white">
                          #{order.id}
                        </span>
                        <span className="px-1.5 py-0.5 rounded-md text-[10px] font-semibold bg-slate-200/60 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                          {tableNumber ? `Bàn ${tableNumber}` : 'Mang đi'}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5">
                        {fmtTime(order.paymentTime || order.createdAt)}
                      </p>
                    </div>
                  </div>

                  <div className="text-right shrink-0 flex flex-col items-end gap-1">
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${statusMeta.classes}`}
                    >
                      {statusMeta.label}
                    </span>
                    <span className="text-xs font-black text-slate-900 dark:text-white font-mono">
                      {fmtVND(orderAmount)}
                    </span>
                  </div>
                </div>
              );
            })}

            {recentOrders.length === 0 && (
              <div className="py-8 text-center text-slate-400 text-xs">
                Chưa phát sinh đơn hàng nào hôm nay.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardOverview;
