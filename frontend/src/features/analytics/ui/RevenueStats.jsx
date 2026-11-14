import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  Tooltip,
} from 'recharts';
import { DollarSign, Loader2, Receipt, ShoppingBag, TrendingUp } from 'lucide-react';
import { analyticsService } from '@entities/analytics/api/analyticsService.js';
import StatsToolbar from '@shared/ui/StatsToolbar.jsx';
import { fmtVND, fmtDate, fmtDateTime } from '@shared/lib/formatters.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import useDateRangeFilter from '../model/useDateRangeFilter.js';

const ITEMS_PER_PAGE = 10;

const RevenueStats = () => {
  const { dateRange, setDateRange, appliedDateRange, handleApplyFilters: applyFilters } =
    useDateRangeFilter();
  const [revenueData, setRevenueData] = useState([]);
  const [orders, setOrders] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingRevenue, setLoadingRevenue] = useState(false);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const isMountedRef = useRef(true);
  const revenueFetchSeqRef = useRef(0);
  const ordersFetchSeqRef = useRef(0);

  const handleApplyFilters = () => applyFilters(() => setCurrentPage(0));

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Gọi API tổng hợp khi áp dụng khoảng ngày
  useEffect(() => {
    const fetchSeq = ++revenueFetchSeqRef.current;
    const loadRevenue = async () => {
      setLoadingRevenue(true);
      try {
        const rev = await analyticsService.getRevenue(appliedDateRange.from, appliedDateRange.to);
        if (!isMountedRef.current || fetchSeq !== revenueFetchSeqRef.current) return;
        setRevenueData(rev);
      } catch (e) {
        console.error(e);
      } finally {
        if (isMountedRef.current && fetchSeq === revenueFetchSeqRef.current) {
          setLoadingRevenue(false);
        }
      }
    };
    loadRevenue();
  }, [appliedDateRange]);

  // Gọi API danh sách giao dịch theo trang
  useEffect(() => {
    const fetchSeq = ++ordersFetchSeqRef.current;
    const loadOrders = async () => {
      setLoadingOrders(true);
      try {
        const orderPage = await analyticsService.getOrders(
          appliedDateRange.from,
          appliedDateRange.to,
          {
            page: currentPage,
            size: ITEMS_PER_PAGE,
          },
        );
        if (!isMountedRef.current || fetchSeq !== ordersFetchSeqRef.current) return;
        setOrders(orderPage.content || []);
        setTotalPages(orderPage.totalPages || 0);
        setTotalElements(orderPage.totalElements || 0);
      } catch (e) {
        console.error(e);
      } finally {
        if (isMountedRef.current && fetchSeq === ordersFetchSeqRef.current) {
          setLoadingOrders(false);
        }
      }
    };
    loadOrders();
  }, [appliedDateRange, currentPage]);

  // Format dữ liệu biểu đồ
  const chartData = useMemo(() => {
    return revenueData.map((item) => {
      const dateKey = fmtDate(new Date(item.bucket + 'T00:00:00'));
      return {
        date: dateKey,
        fullDate: fmtDateTime(new Date(item.bucket + 'T00:00:00')),
        revenue: Number(item.revenue || 0),
        orderCount: Number(item.orders || 0),
      };
    });
  }, [revenueData]);

  // Tính toán KPI
  const kpi = useMemo(() => {
    const totalRev = revenueData.reduce((s, i) => s + Number(i.revenue || 0), 0);
    const totalOrd = revenueData.reduce((s, i) => s + Number(i.orders || 0), 0);
    const avg = totalOrd ? Math.round(totalRev / totalOrd) : 0;
    return { totalRev, totalOrd, avg };
  }, [revenueData]);

  useEffect(() => {
    if (totalPages > 0 && currentPage >= totalPages) {
      setCurrentPage(totalPages - 1);
    } else if (totalPages === 0 && currentPage !== 0) {
      setCurrentPage(0);
    }
  }, [currentPage, totalPages]);

  const initialLoading =
    (loadingRevenue || loadingOrders) && revenueData.length === 0 && orders.length === 0;

  return (
    <div className="w-full min-w-0 space-y-6 p-0 sm:p-3 lg:p-6">
      <StatsToolbar
        dateRange={dateRange}
        setDateRange={setDateRange}
        onApply={handleApplyFilters}
        title="Thời gian"
      />

      {initialLoading ? (
        <div className="p-20 text-center">
          <Loader2 className="animate-spin inline text-indigo-600 dark:text-indigo-400" size={36} />
        </div>
      ) : (
        <div className="space-y-6">
          {/* 1. KPI Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="flex items-center gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
                <DollarSign size={22} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                  Tổng doanh thu
                </p>
                <p className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white truncate">
                  {fmtVND(kpi.totalRev)}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/25">
                <ShoppingBag size={22} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                  Tổng đơn hàng
                </p>
                <p className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white truncate">
                  {kpi.totalOrd}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/25">
                <TrendingUp size={22} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                  Giá trị TB / Đơn
                </p>
                <p className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white truncate">
                  {fmtVND(kpi.avg)}
                </p>
              </div>
            </div>
          </div>

          {/* 2. Biểu đồ */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Biểu đồ miền (Doanh thu) */}
            <div className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90">
              <h3 className="font-bold text-slate-900 mb-6 dark:text-white">Xu hướng doanh thu</h3>
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <defs>
                      <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366F1" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#6366F1" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis
                      dataKey="date"
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: '#94a3b8', fontSize: 12 }}
                      dy={10}
                    />
                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: '#94a3b8', fontSize: 12 }}
                      tickFormatter={(v) => `${v / 1000}k`}
                    />
                    <RechartsTooltip
                      formatter={(v) => [fmtVND(v), 'Doanh thu']}
                      labelFormatter={(l, p) => p[0]?.payload.fullDate}
                      contentStyle={{
                        borderRadius: '16px',
                        border: '1px solid rgba(226, 232, 240, 0.8)',
                        boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)',
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="revenue"
                      stroke="#6366F1"
                      fill="url(#colorRev)"
                      strokeWidth={3}
                      activeDot={{ r: 6, fill: '#6366F1' }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Biểu đồ cột (Số lượng đơn) */}
            <div className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90">
              <h3 className="font-bold text-slate-900 mb-6 dark:text-white">Số lượng đơn hàng</h3>
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis
                      dataKey="date"
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: '#94a3b8', fontSize: 12 }}
                      dy={10}
                    />
                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: '#94a3b8', fontSize: 12 }}
                      allowDecimals={false}
                    />
                    <Tooltip
                      cursor={{ fill: '#f8fafc' }}
                      formatter={(v) => [v, 'Đơn hàng']}
                      labelFormatter={(l, p) => p[0]?.payload.fullDate}
                      contentStyle={{
                        borderRadius: '16px',
                        border: '1px solid rgba(226, 232, 240, 0.8)',
                        boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)',
                      }}
                    />
                    <Bar dataKey="orderCount" fill="#8B5CF6" radius={[6, 6, 0, 0]} barSize={36} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* 3. BẢNG CHI TIẾT GIAO DỊCH */}
          <div className="bg-white/90 rounded-3xl shadow-sm border border-[#1E293B] backdrop-blur-sm overflow-hidden dark:bg-[#0F172A]/90 dark:border-[#1E293B]">
            <div className="p-6 border-b border-slate-200/80 flex items-center justify-between dark:border-slate-800/80">
              <h3 className="font-bold text-slate-900 flex items-center gap-2.5 dark:text-white">
                <Receipt size={20} className="text-indigo-600 dark:text-indigo-400" />
                Chi tiết giao dịch
              </h3>
              <span className="text-xs font-bold font-mono text-slate-500 dark:text-slate-400">
                {totalElements} đơn hàng
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead className="bg-slate-50/80 text-slate-500 text-[11px] uppercase font-bold tracking-wider dark:bg-slate-950/80 dark:text-slate-400 border-b border-slate-200/80 dark:border-slate-800/80">
                  <tr>
                    <th className="p-4">Mã đơn</th>
                    <th className="p-4">Thời gian</th>
                    <th className="p-4">Nhân viên</th>
                    <th className="p-4 text-center">Trạng thái</th>
                    <th className="p-4 text-right">Tổng tiền</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {loadingOrders ? (
                    <tr key="loading-revenue-orders">
                      <td colSpan="5" className="p-12 text-center text-slate-500 dark:text-slate-400">
                        <Loader2 className="animate-spin inline mr-2 text-indigo-600" size={18} />
                        Đang tải giao dịch...
                      </td>
                    </tr>
                  ) : orders.length === 0 ? (
                    <tr key="empty-revenue-row">
                      <td colSpan="5" className="p-12 text-center text-slate-400 dark:text-slate-500 italic">
                        Không có dữ liệu giao dịch trong khoảng thời gian này.
                      </td>
                    </tr>
                  ) : (
                    orders.map((order) => (
                      <tr
                        key={order.id}
                        className="border-b border-slate-100 last:border-0 hover:bg-indigo-50/30 transition-colors dark:border-slate-800/60 dark:hover:bg-slate-800/40"
                      >
                        <td className="p-4 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                          #{order.id}
                        </td>
                        <td className="p-4 text-xs font-medium text-slate-600 dark:text-slate-300">
                          {fmtDateTime(order.paymentTime || order.createdAt)}
                        </td>
                        <td className="p-4 text-xs font-bold text-slate-800 dark:text-slate-100">
                          {order.empName || '—'}
                        </td>
                        <td className="p-4 text-center">
                          <span className="px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200/60 rounded-full text-[11px] font-bold dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800/40">
                            Đã thanh toán
                          </span>
                        </td>
                        <td className="p-4 text-right font-mono font-black text-slate-900 dark:text-white">
                          {fmtVND(getOrderFinalAmount(order))}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
              {totalPages > 1 && (
                <div className="flex justify-center items-center gap-3 p-4 border-t border-slate-200/80 dark:border-slate-800/80">
                  <button
                    type="button"
                    disabled={loadingOrders || currentPage === 0}
                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 0))}
                    className="px-4 py-2 rounded-xl border border-slate-200/80 bg-white text-xs font-bold text-slate-600 hover:border-indigo-200 hover:bg-indigo-50/50 hover:text-indigo-600 disabled:opacity-50 transition-all shadow-sm active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300"
                  >
                    Trước
                  </button>
                  <span className="text-slate-500 text-xs font-bold px-4 dark:text-slate-400">
                    Trang {currentPage + 1} / {totalPages}
                    <span className="text-slate-400 ml-2 font-mono text-xs dark:text-slate-500">
                      ({totalElements} đơn)
                    </span>
                  </span>
                  <button
                    type="button"
                    disabled={loadingOrders || currentPage >= totalPages - 1}
                    onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages - 1))}
                    className="px-4 py-2 rounded-xl border border-slate-200/80 bg-white text-xs font-bold text-slate-600 hover:border-indigo-200 hover:bg-indigo-50/50 hover:text-indigo-600 disabled:opacity-50 transition-all shadow-sm active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300"
                  >
                    Sau
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RevenueStats;
