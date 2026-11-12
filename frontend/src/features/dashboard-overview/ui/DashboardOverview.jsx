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
  getDemandLevel,
} from '@shared/lib/analytics/forecastUtils.js';
import useDashboard from '../model/useDashboard.js';

const COLORS_PIE = ['#C17C4A', '#56372B', '#10B981', '#2563EB', '#8B5CF6', '#D97706'];

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
        <Loader2 className="animate-spin text-[#C17C4A] w-12 h-12" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[420px] items-center justify-center p-6">
        <div className="rounded-3xl border border-rose-200 dark:border-rose-900/60 bg-rose-50 dark:bg-rose-950/30 p-8 text-center max-w-sm shadow-sm">
          <AlertCircle className="mx-auto mb-3 text-rose-500" size={32} />
          <p className="font-bold text-rose-700 dark:text-rose-400 mb-1">Không thể tải dữ liệu</p>
          <p className="text-xs text-rose-600 dark:text-rose-400 mb-4">Vui lòng kiểm tra kết nối và thử lại.</p>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 text-sm font-bold text-white hover:bg-rose-700 transition-colors shadow-sm"
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
    <div className="min-h-screen min-w-0 bg-transparent p-0 sm:p-3 lg:p-6">
      {/* 1. KPI Cards */}
      <div className="mt-3 grid min-w-0 grid-cols-1 gap-4 sm:mt-6 sm:gap-6 md:grid-cols-2 lg:grid-cols-4">
        {/* Revenue Card */}
        <div className="bg-white dark:bg-[#1F1A17] rounded-3xl p-6 shadow-sm border border-[#E8DCD1] dark:border-[#352C26] flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-[#6B5D55] dark:text-[#BDB0A4] text-xs font-bold uppercase tracking-wider mb-1">Doanh thu (Hôm nay)</p>
              <h3 className="text-2xl font-black text-[#2D1D16] dark:text-[#F7F0E7]">{fmtVND(todayRevenue)}</h3>
            </div>
            <div className="w-11 h-11 rounded-2xl bg-[#F7F0E7] dark:bg-[#2A221E] flex items-center justify-center text-[#C17C4A] shadow-sm">
              <TrendingUp size={22} />
            </div>
          </div>
        </div>

        {/* Orders Card */}
        <div className="bg-white dark:bg-[#1F1A17] rounded-3xl p-6 shadow-sm border border-[#E8DCD1] dark:border-[#352C26] flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-[#6B5D55] dark:text-[#BDB0A4] text-xs font-bold uppercase tracking-wider mb-1">Đơn hàng (Hôm nay)</p>
              <h3 className="text-2xl font-black text-[#2D1D16] dark:text-[#F7F0E7]">{todayOrders.total}</h3>
            </div>
            <div className="w-11 h-11 rounded-2xl bg-blue-50 dark:bg-blue-500/10 flex items-center justify-center text-blue-600 dark:text-blue-400 shadow-sm">
              <ShoppingBag size={22} />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2 text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4]">
            <span className="text-emerald-600 dark:text-emerald-400 font-bold">{todayOrders.completed}</span> hoàn thành
          </div>
        </div>

        {/* Tables Card */}
        <div className="bg-white dark:bg-[#1F1A17] rounded-3xl p-6 shadow-sm border border-[#E8DCD1] dark:border-[#352C26] flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-[#6B5D55] dark:text-[#BDB0A4] text-xs font-bold uppercase tracking-wider mb-1">Bàn hoạt động</p>
              <h3 className="text-2xl font-black text-[#2D1D16] dark:text-[#F7F0E7]">
                {tablesContext.occupied}{' '}
                <span className="text-base text-[#9C8E85] dark:text-[#82756B] font-bold">/ {tablesContext.total}</span>
              </h3>
            </div>
            <div className="w-11 h-11 rounded-2xl bg-emerald-50 dark:bg-emerald-500/10 flex items-center justify-center text-emerald-600 dark:text-emerald-400 shadow-sm">
              <UtensilsCrossed size={22} />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2 text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4]">
            Đang có khách ngồi
          </div>
        </div>

        {/* Avg Order Value */}
        <div className="bg-white dark:bg-[#1F1A17] rounded-3xl p-6 shadow-sm border border-[#E8DCD1] dark:border-[#352C26] flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-[#6B5D55] dark:text-[#BDB0A4] text-xs font-bold uppercase tracking-wider mb-1">Giá trị TB / đơn</p>
              <h3 className="text-2xl font-black text-[#2D1D16] dark:text-[#F7F0E7]">{fmtVND(avgOrderValue)}</h3>
            </div>
            <div className="w-11 h-11 rounded-2xl bg-[#F7F0E7] dark:bg-[#2A221E] flex items-center justify-center text-[#56372B] dark:text-[#D8986A] shadow-sm">
              <Banknote size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Forecasting Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="min-w-0 rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] p-5 shadow-sm sm:p-7 lg:col-span-2">
          <div className="mb-6">
            <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7] flex items-center gap-2 mb-1">
              <TrendingUp size={20} className="text-[#C17C4A]" /> Dự báo doanh thu 7 ngày tới
            </h3>
          </div>

          {/* Forecast KPI Summary */}
          {revenueForecastData.length > 0 && (
            <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="rounded-2xl bg-[#F7F0E7] p-3.5 dark:bg-[#2A221E] border border-[#E8DCD1] dark:border-[#352C26]">
                <p className="text-xs font-bold text-[#6B5D55] dark:text-[#BDB0A4] uppercase tracking-wider">
                  Dự báo ngày mai
                </p>
                <p className="mt-1 text-lg font-black text-[#56372B] dark:text-[#D8986A]">
                  {fmtVND(forecastSummary.forecastTomorrow)}
                </p>
              </div>

              <div className="rounded-2xl bg-white p-3.5 dark:bg-[#150E0B] border border-[#E8DCD1] dark:border-[#352C26]">
                <p className="text-xs font-bold text-[#6B5D55] dark:text-[#BDB0A4] uppercase tracking-wider">
                  So với TB 7 ngày
                </p>
                <p className="mt-1 text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">
                  {forecastSummary.diffPercent >= 0 ? '+' : ''}
                  {Math.round(forecastSummary.diffPercent)}%
                </p>
              </div>

              <div className="rounded-2xl bg-emerald-50 p-3.5 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40">
                <p className="text-xs font-bold text-emerald-800 dark:text-emerald-300 uppercase tracking-wider">Xu hướng</p>
                <p className="mt-1 text-lg font-black text-emerald-700 dark:text-emerald-400">
                  {getTrendLabel(forecastSummary.trend)}
                </p>
              </div>
            </div>
          )}

          <div className="flex items-center gap-4 text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4] mb-3">
            <span className="flex items-center gap-2">
              <span className="w-3 h-1 rounded-full bg-emerald-500"></span> Thực tế
            </span>
            <span className="flex items-center gap-2">
              <span className="w-3 h-0.5 border-t-2 border-dashed border-[#C17C4A]"></span> Dự báo AI
            </span>
          </div>

          <div className="h-64">
            {revenueForecastData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={revenueForecastData}
                  margin={{ top: 8, right: 16, bottom: 8, left: 8 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#E8DCD1" opacity={0.6} vertical={false} />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 11, fill: '#82756B' }}
                    minTickGap={18}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tickFormatter={fmtCompactVND}
                    tick={{ fontSize: 11, fill: '#82756B' }}
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
                    stroke="#C17C4A"
                    strokeWidth={3}
                    strokeDasharray="6 6"
                    dot={{ r: 3, fill: '#C17C4A' }}
                    connectNulls={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-[#9C8E85] dark:text-[#82756B]">
                Chưa có dữ liệu dự báo doanh thu
              </div>
            )}
          </div>
        </div>

        <div className="min-w-0 rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] p-5 shadow-sm sm:p-7">
          <div className="mb-6">
            <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7] flex items-center gap-2 mb-1">
              <Sparkles size={20} className="text-[#C17C4A]" /> Gợi ý chuẩn bị món
            </h3>
            <p className="text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4]">Dựa trên xu hướng gọi món 30 ngày qua</p>
          </div>
          <div className="flex flex-col gap-3">
            {popularDishesForecast.map((dish, idx) => {
              const demand = getDemandLevel(dish.estimatedQty);
              return (
                <div
                  key={dish.id || idx}
                  className="rounded-2xl border border-[#E8DCD1] bg-[#F7F0E7]/40 p-4 shadow-sm dark:border-[#352C26] dark:bg-[#150E0B]"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex items-center gap-3">
                      <div
                        className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-black text-white shadow-sm shrink-0 ${idx === 0 ? 'bg-[#56372B]' : idx === 1 ? 'bg-[#C17C4A]' : idx === 2 ? 'bg-[#D8986A]' : 'bg-slate-400'}`}
                      >
                        {idx + 1}
                      </div>
                      <div className="min-w-0">
                        <p
                          className="truncate font-bold text-sm text-[#2D1D16] dark:text-[#F7F0E7]"
                          title={dish.name}
                        >
                          {dish.name}
                        </p>
                        <p className="mt-0.5 text-xs text-[#6B5D55] dark:text-[#BDB0A4]">
                          Dự kiến bán:{' '}
                          <span className="font-bold text-[#56372B] dark:text-[#D8986A]">{dish.estimatedQty}</span> suất
                        </p>
                      </div>
                    </div>

                    <span
                      className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] uppercase font-black tracking-wide ${demand.className}`}
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
              <p className="text-center text-[#9C8E85] dark:text-[#82756B] py-4">Chưa có dữ liệu dự báo món.</p>
            )}
          </div>
        </div>
      </div>

      {/* 2 & 4. Charts and Leaderboard Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        {/* Tỉ trọng danh mục (Pie Chart) */}
        <div className="min-w-0 rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] p-5 shadow-sm sm:p-7">
          <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7] mb-6">
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
              <div className="h-full flex items-center justify-center text-[#9C8E85] dark:text-[#82756B]">
                Chưa có thông tin bán hàng
              </div>
            )}
          </div>
        </div>

        {/* Top 5 Món Bán Chạy */}
        <div className="min-w-0 rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] p-5 shadow-sm sm:p-7">
          <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7] mb-6 flex items-center gap-2">
            <UtensilsCrossed size={20} className="text-[#C17C4A]" /> Top món (7 ngày)
          </h3>
          <div className="flex flex-col gap-3">
            {topDishes.map((dish, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-[#F7F0E7]/40 dark:bg-[#150E0B] border border-[#E8DCD1] dark:border-[#352C26] transition-colors"
              >
                <div className="flex items-center gap-3.5 min-w-0">
                  <div
                    className={`w-8 h-8 rounded-xl flex items-center justify-center text-xs font-black text-white shadow-sm shrink-0 ${idx === 0 ? 'bg-[#56372B]' : idx === 1 ? 'bg-[#C17C4A]' : idx === 2 ? 'bg-[#D8986A]' : 'bg-slate-400'}`}
                  >
                    {idx + 1}
                  </div>
                  <div className="min-w-0">
                    <p className="font-bold text-sm text-[#2D1D16] dark:text-[#F7F0E7] truncate" title={dish.name}>
                      {dish.name}
                    </p>
                    <p className="text-xs text-[#6B5D55] dark:text-[#BDB0A4]">
                      Đã bán: <span className="font-bold">{dish.totalQty}</span>
                    </p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-sm font-black text-[#56372B] dark:text-[#D8986A]">{fmtVND(dish.totalRevenue)}</p>
                </div>
              </div>
            ))}
            {topDishes.length === 0 && (
              <p className="text-center text-[#9C8E85] dark:text-[#82756B] py-4">Chưa có món nào được bán ra.</p>
            )}
          </div>
        </div>

        {/* Giao dịch gần nhất */}
        <div className="min-w-0 rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] p-5 shadow-sm sm:p-7">
          <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7] mb-6 flex items-center gap-2">
            <Clock size={20} className="text-[#C17C4A]" /> Đơn hàng mới nhất
          </h3>
          <div className="flex flex-col gap-4">
            {recentOrders.map((order, idx) => {
              const tableNumber = order.table?.tableNumber || order.tableNumber;
              const orderAmount = order.finalAmount ?? getOrderFinalAmount(order);

              return (
                <div
                  key={order.id || idx}
                  className="flex justify-between items-start p-3 border-b border-slate-50 dark:border-slate-800 last:border-0 hover:bg-slate-50 dark:hover:bg-slate-800/50 rounded-xl transition-colors"
                >
                  <div>
                    <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">Đơn #{order.id}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
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
                    <span className="text-sm font-bold text-slate-800 dark:text-slate-100">{fmtVND(orderAmount)}</span>
                  </div>
                </div>
              );
            })}
            {recentOrders.length === 0 && (
              <p className="text-center text-slate-400 dark:text-slate-500 py-4">Hôm nay chưa có đơn hàng nào.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
