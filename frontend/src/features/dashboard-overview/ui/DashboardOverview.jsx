import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
  ResponsiveContainer,
  LineChart,
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
} from 'lucide-react';
import { fmtVND, fmtTime, fmtCompactVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import {
  getForecastSummary,
  getTrendLabel,
  getRevenueInsight,
  getDemandLevel,
} from '@shared/lib/analytics/forecastUtils.js';
import useDashboard from '../model/useDashboard.js';

const COLORS_PIE = ['#f97316', '#3b82f6', '#10b981', '#14b8a6', '#ec4899', '#f59e0b'];

const Dashboard = () => {
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

  if (loading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center">
        <Loader2 className="animate-spin text-orange-500 w-12 h-12" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[420px] items-center justify-center p-6">
        <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center max-w-sm">
          <AlertCircle className="mx-auto mb-3 text-red-400" size={32} />
          <p className="font-semibold text-red-700 mb-1">Không thể tải dữ liệu</p>
          <p className="text-sm text-red-500 mb-4">Vui lòng kiểm tra kết nối và thử lại.</p>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors"
          >
            <RefreshCw size={14} />
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  const forecastSummary = getForecastSummary(revenueForecastData);

  return (
    <div className="min-h-screen min-w-0 bg-slate-50 p-0 sm:p-3 lg:p-6">
      {/* 1. KPI Cards */}
      <div className="mt-3 grid min-w-0 grid-cols-1 gap-4 sm:mt-6 sm:gap-6 md:grid-cols-2 lg:grid-cols-4">
        {/* Revenue Card */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-slate-500 text-sm font-medium mb-1">Doanh thu (Hôm nay)</p>
              <h3 className="text-2xl font-bold text-slate-800">{fmtVND(todayRevenue)}</h3>
            </div>
            <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center text-orange-600">
              <TrendingUp size={20} />
            </div>
          </div>
        </div>

        {/* Orders Card */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-slate-500 text-sm font-medium mb-1">Đơn hàng (Hôm nay)</p>
              <h3 className="text-2xl font-bold text-slate-800">{todayOrders.total}</h3>
            </div>
            <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600">
              <ShoppingBag size={20} />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2 text-sm text-slate-600">
            <span className="text-green-600 font-semibold">{todayOrders.completed}</span> hoàn thành
          </div>
        </div>

        {/* Tables Card */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-slate-500 text-sm font-medium mb-1">Bàn</p>
              <h3 className="text-2xl font-bold text-slate-800">
                {tablesContext.occupied}{' '}
                <span className="text-lg text-slate-400 font-medium">/ {tablesContext.total}</span>
              </h3>
            </div>
            <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
              <UtensilsCrossed size={20} />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2 text-sm text-slate-600">
            Đang có khách ngồi
          </div>
        </div>

        {/* Avg Order Value */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-slate-500 text-sm font-medium mb-1">Giá trị trung bình đơn</p>
              <h3 className="text-2xl font-bold text-slate-800">{fmtVND(avgOrderValue)}</h3>
            </div>
            <div className="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center text-teal-600">
              <Banknote size={20} />
            </div>
          </div>
        </div>
      </div>

      {/* Forecasting Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="min-w-0 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm sm:p-6 lg:col-span-2">
          <div className="mb-6">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-1">
              <TrendingUp size={18} className="text-orange-500" /> Dự báo doanh thu 7 ngày tới
            </h3>
          </div>

          {/* Forecast KPI Summary */}
          {revenueForecastData.length > 0 && (
            <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="rounded-xl bg-orange-50 p-3 dark:bg-orange-950/30">
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400">
                  Dự báo ngày mai
                </p>
                <p className="mt-1 text-lg font-bold text-orange-600 dark:text-orange-400">
                  {fmtVND(forecastSummary.forecastTomorrow)}
                </p>
              </div>

              <div className="rounded-xl bg-slate-50 p-3 dark:bg-slate-800/70">
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400">
                  So với trung bình 7 ngày
                </p>
                <p className="mt-1 text-lg font-bold text-slate-800 dark:text-slate-100">
                  {forecastSummary.diffPercent >= 0 ? '+' : ''}
                  {Math.round(forecastSummary.diffPercent)}%
                </p>
              </div>

              <div className="rounded-xl bg-emerald-50 p-3 dark:bg-emerald-950/30">
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Xu hướng</p>
                <p className="mt-1 text-lg font-bold text-emerald-600 dark:text-emerald-400">
                  {getTrendLabel(forecastSummary.trend)}
                </p>
              </div>
            </div>
          )}

          <div className="flex items-center gap-4 text-xs font-medium text-slate-500 mb-3">
            <span className="flex items-center gap-2">
              <span className="w-3 h-0.5 bg-emerald-500"></span> Thực tế
            </span>
            <span className="flex items-center gap-2">
              <span className="w-3 h-0.5 border-t-2 border-dashed border-amber-500"></span> Dự báo
            </span>
          </div>

          <div className="h-64">
            {revenueForecastData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={revenueForecastData}
                  margin={{ top: 8, right: 16, bottom: 8, left: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 12, fill: '#64748b' }}
                    minTickGap={18}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tickFormatter={fmtCompactVND}
                    tick={{ fontSize: 12, fill: '#64748b' }}
                    axisLine={false}
                    tickLine={false}
                    width={52}
                  />
                  <Tooltip
                    formatter={(value, name) => [
                      fmtVND(value),
                      name === 'actual' ? 'Thực tế' : 'Dự báo',
                    ]}
                    labelFormatter={(_, payload) => payload?.[0]?.payload?.date || ''}
                  />
                  <Line
                    type="monotone"
                    dataKey="actual"
                    stroke="#10b981"
                    strokeWidth={3}
                    dot={false}
                    connectNulls={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="forecast"
                    stroke="#f59e0b"
                    strokeWidth={3}
                    strokeDasharray="6 6"
                    dot={{ r: 3 }}
                    connectNulls={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-400">
                Chưa có dữ liệu dự báo doanh thu
              </div>
            )}
          </div>
        </div>

        <div className="min-w-0 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm sm:p-6">
          <div className="mb-6">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-1">
              <Sparkles size={18} className="text-amber-500" /> Gợi ý chuẩn bị món
            </h3>
            <p className="text-sm text-slate-500">Dựa trên xu hướng bán ra 30 ngày qua</p>
          </div>
          <div className="flex flex-col gap-4">
            {popularDishesForecast.map((dish, idx) => {
              const demand = getDemandLevel(dish.estimatedQty);
              return (
                <div
                  key={dish.id || idx}
                  className="rounded-xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex items-center gap-3">
                      <div
                        className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold text-white shadow-sm shrink-0 ${idx === 0 ? 'bg-amber-500' : idx === 1 ? 'bg-orange-500' : idx === 2 ? 'bg-emerald-500' : 'bg-slate-300'}`}
                      >
                        {idx + 1}
                      </div>
                      <div className="min-w-0">
                        <p
                          className="truncate font-semibold text-slate-800 dark:text-slate-100"
                          title={dish.name}
                        >
                          {dish.name}
                        </p>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          Dự kiến bán ra:{' '}
                          <span className="font-bold text-slate-700">{dish.estimatedQty}</span> suất
                        </p>
                      </div>
                    </div>

                    <span
                      className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] uppercase font-bold tracking-wide ${demand.className}`}
                    >
                      {demand.label}
                    </span>
                  </div>

                  <div className="mt-3 h-1.5 rounded-full bg-slate-100 dark:bg-slate-800 overflow-hidden">
                    <div
                      className={`h-full bg-${demand.color}-500`}
                      style={{
                        width: `${Math.min(100, (dish.estimatedQty / 30) * 100)}%`,
                      }}
                    />
                  </div>
                </div>
              );
            })}
            {popularDishesForecast.length === 0 && (
              <p className="text-center text-slate-400 py-4">Chưa có dữ liệu dự báo món.</p>
            )}
          </div>
        </div>
      </div>

      {/* 2 & 4. Charts and Leaderboard Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        {/* Tỉ trọng danh mục (Pie Chart) */}
        <div className="min-w-0 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm sm:p-6">
          <h3 className="text-lg font-bold text-slate-800 mb-6">
            Tỉ trọng danh mục (7 ngày)
          </h3>
          <div className="h-72">
            {categoryShares.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryShares}
                    cx="50%"
                    cy="45%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {categoryShares.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS_PIE[index % COLORS_PIE.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => fmtVND(value)} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-400">
                Chưa có thông tin bán hàng
              </div>
            )}
          </div>
        </div>

        {/* Top 5 Món Bán Chạy */}
        <div className="min-w-0 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm sm:p-6">
          <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
            <UtensilsCrossed size={18} className="text-emerald-500" /> Top món (7 ngày)
          </h3>
          <div className="flex flex-col gap-4">
            {topDishes.map((dish, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent shadow-sm"
              >
                <div className="flex items-center gap-4 min-w-0">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold text-white shadow-sm shrink-0 ${idx === 0 ? 'bg-orange-500' : idx === 1 ? 'bg-blue-500' : idx === 2 ? 'bg-emerald-500' : 'bg-slate-300'}`}
                  >
                    {idx + 1}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-slate-800 truncate" title={dish.name}>
                      {dish.name}
                    </p>
                    <p className="text-xs text-slate-500">
                      Đã bán: <span className="font-medium">{dish.totalQty}</span>
                    </p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-sm font-bold text-slate-800">{fmtVND(dish.totalRevenue)}</p>
                </div>
              </div>
            ))}
            {topDishes.length === 0 && (
              <p className="text-center text-slate-400 py-4">Chưa có món nào được bán ra.</p>
            )}
          </div>
        </div>

        {/* Giao dịch gần nhất */}
        <div className="min-w-0 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm sm:p-6">
          <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
            <Clock size={18} className="text-blue-500" /> Đơn hàng mới nhất
          </h3>
          <div className="flex flex-col gap-4">
            {recentOrders.map((order, idx) => {
              const tableNumber = order.table?.tableNumber || order.tableNumber;
              const orderAmount = order.finalAmount ?? getOrderFinalAmount(order);

              return (
                <div
                  key={order.id || idx}
                  className="flex justify-between items-start p-3 border-b border-slate-50 last:border-0 hover:bg-slate-50 rounded-xl transition-colors"
                >
                  <div>
                    <p className="text-sm font-semibold text-slate-700">Đơn #{order.id}</p>
                    <p className="text-xs text-slate-500">
                      {tableNumber ? `Bàn ${tableNumber}` : 'Mang đi'} •{' '}
                      {fmtTime(order.paymentTime || order.createdAt)}
                    </p>
                  </div>
                  <div className="text-right flex flex-col items-end gap-1">
                    <span
                      className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${getOrderStatusMeta(order.status).classes}`}
                    >
                      {getOrderStatusMeta(order.status).label}
                    </span>
                    <span className="text-sm font-bold text-slate-800">{fmtVND(orderAmount)}</span>
                  </div>
                </div>
              );
            })}
            {recentOrders.length === 0 && (
              <p className="text-center text-slate-400 py-4">Hôm nay chưa có đơn hàng nào.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
