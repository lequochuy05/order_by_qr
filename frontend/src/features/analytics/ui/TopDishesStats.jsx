import React, { useMemo, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  Activity,
  BarChart3,
  Flame,
  ShoppingBag,
  TrendingUp,
  UtensilsCrossed,
} from 'lucide-react';
import { analyticsService } from '@entities/analytics';
import { queryKeys } from '@shared/api/queryKeys.js';
import StatsToolbar from '@shared/ui/StatsToolbar.jsx';
import { fmtDate, fmtVND } from '@shared/lib/formatters.js';
import { formatBusinessDate } from '@shared/lib/businessTime.js';
import useDateRangeFilter from '../model/useDateRangeFilter.js';
import TopDishesChartsSection from './TopDishesChartsSection.jsx';
import TopDishesTableSection from './TopDishesTableSection.jsx';

const ITEMS_PER_PAGE = 10;
const EMPTY_LIST = [];

const TopDishesStats = () => {
  const { dateRange, setDateRange, appliedDateRange, handleApplyFilters: applyFilters } =
    useDateRangeFilter();
  const [page, setCurrentPage] = useState(0);

  const handleApplyFilters = () => applyFilters(() => setCurrentPage(0));

  const { data: dishes = EMPTY_LIST, isPending: loadingDishes } = useQuery({
    queryKey: queryKeys.analytics.topDishes(appliedDateRange.from, appliedDateRange.to),
    queryFn: () => analyticsService.getTopDishes(appliedDateRange.from, appliedDateRange.to),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });

  const { data: trend = EMPTY_LIST, isPending: loadingTrend } = useQuery({
    queryKey: queryKeys.analytics.dishTrend(appliedDateRange.from, appliedDateRange.to),
    queryFn: () => analyticsService.getDishTrend(appliedDateRange.from, appliedDateRange.to),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });

  const loading = loadingDishes || loadingTrend;

  const kpi = useMemo(() => {
    const totalDishes = dishes.length;
    const totalQty = dishes.reduce((s, d) => s + (d.totalQty || 0), 0);
    const totalRev = dishes.reduce((s, d) => s + (d.totalRevenue || 0), 0);
    const avg = totalDishes ? Math.round(totalQty / totalDishes) : 0;
    return { totalDishes, totalQty, totalRev, avg };
  }, [dishes]);

  const barData = useMemo(
    () =>
      dishes.slice(0, 10).map((d) => ({
        name: d.name.length > 16 ? d.name.substring(0, 16) + '...' : d.name,
        fullName: d.name,
        quantity: d.totalQty,
        revenue: d.totalRevenue,
      })),
    [dishes],
  );

  const trendData = useMemo(
    () =>
      trend.map((t) => ({
        date: fmtDate(t.bucket),
        quantity: t.totalQty,
      })),
    [trend],
  );

  const maxQty = dishes.length > 0 ? dishes[0].totalQty : 1;
  const totalPages = Math.ceil(dishes.length / ITEMS_PER_PAGE);
  const currentPage = totalPages > 0 ? Math.min(page, totalPages - 1) : 0;
  const paginatedDishes = useMemo(() => {
    const start = currentPage * ITEMS_PER_PAGE;
    return dishes.slice(start, start + ITEMS_PER_PAGE);
  }, [currentPage, dishes]);

  const handleExport = () => {
    const header = 'Hạng,Tên món,Danh mục,SL bán,Doanh thu';
    const escapeCsvValue = (value) => `"${String(value ?? '').replace(/"/g, '""')}"`;
    const rows = dishes
      .map((d, i) =>
        [
          i + 1,
          escapeCsvValue(d.name),
          escapeCsvValue(d.categoryName || d.category || ''),
          d.totalQty || 0,
          d.totalRevenue || 0,
        ].join(','),
      )
      .join('\n');
    const totalRow = [
      '',
      escapeCsvValue('Tổng số lượng món bán / Tổng doanh thu'),
      '',
      kpi.totalQty,
      kpi.totalRev,
    ].join(',');
    const content = ['\uFEFF' + header, rows, totalRow].filter(Boolean).join('\n');
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `top-dishes-${formatBusinessDate(appliedDateRange.from)}_${formatBusinessDate(appliedDateRange.to)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="w-full min-w-0 space-y-6 lg:space-y-8 animate-fade-in-up">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-[#1E293B]/20 dark:border-[#1E293B]">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Thống kê món ăn bán chạy
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-400 border border-amber-200/60 dark:border-amber-800/40">
              <Flame size={12} className="text-amber-500" />
              Best Sellers
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Phân tích sản lượng tiêu thụ, doanh số đóng góp và xu hướng thị hiếu khách hàng.
          </p>
        </div>
      </div>

      {/* Date Filter Toolbar */}
      <StatsToolbar
        dateRange={dateRange}
        setDateRange={setDateRange}
        onApply={handleApplyFilters}
        title="Thời gian"
        onExport={handleExport}
      />

      {loading ? (
        <div className="flex min-h-[420px] flex-col items-center justify-center gap-3">
          <div className="relative flex items-center justify-center">
            <div className="w-14 h-14 rounded-full border-4 border-indigo-100 dark:border-indigo-950/60 border-t-indigo-600 dark:border-t-indigo-500 animate-spin" />
            <Activity className="absolute text-indigo-600 dark:text-indigo-400 w-6 h-6 animate-pulse" />
          </div>
          <p className="text-sm font-semibold text-slate-500 dark:text-slate-400">
            Đang tải dữ liệu thống kê món ăn...
          </p>
        </div>
      ) : (
        <div className="space-y-6 lg:space-y-8">
          {/* 1. 4 Stats KPI Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
            <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Tổng số món đã bán
                  </p>
                  <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                    {kpi.totalDishes}
                  </h3>
                </div>
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:scale-110 transition-transform duration-300">
                  <UtensilsCrossed size={22} />
                </div>
              </div>
              <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800/60 text-xs text-slate-500 dark:text-slate-400">
                <span className="font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded-md">
                  Mặt hàng
                </span>
                <span>Món có phát sinh đơn bán</span>
              </div>
            </div>

            <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Tổng sản lượng bán ra
                  </p>
                  <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                    {kpi.totalQty.toLocaleString()}
                  </h3>
                </div>
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-cyan-500 text-white flex items-center justify-center shadow-lg shadow-blue-500/25 group-hover:scale-110 transition-transform duration-300">
                  <ShoppingBag size={22} />
                </div>
              </div>
              <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
                <span className="font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 px-2 py-0.5 rounded-md">
                  Suất ăn
                </span>
                <span>Lượt phục vụ thực tế</span>
              </div>
            </div>

            <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Doanh thu món ăn
                  </p>
                  <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                    {fmtVND(kpi.totalRev)}
                  </h3>
                </div>
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/25 group-hover:scale-110 transition-transform duration-300">
                  <TrendingUp size={22} />
                </div>
              </div>
              <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800/60 text-xs text-slate-500 dark:text-slate-400">
                <span className="font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded-md">
                  Doanh số
                </span>
                <span>Tổng giá trị món bán</span>
              </div>
            </div>

            <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    Trung bình SL / Món
                  </p>
                  <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
                    {kpi.avg}
                    <span className="text-base font-bold text-slate-400 ml-1">suất</span>
                  </h3>
                </div>
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-500 text-white flex items-center justify-center shadow-lg shadow-amber-500/25 group-hover:scale-110 transition-transform duration-300">
                  <BarChart3 size={22} />
                </div>
              </div>
              <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
                <span className="font-semibold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2 py-0.5 rounded-md">
                  Hiệu suất
                </span>
                <span>Sức tiêu thụ trung bình</span>
              </div>
            </div>
          </div>

          {/* 2. Top 10 BarChart & Daily Trend AreaChart */}
          <TopDishesChartsSection barData={barData} trendData={trendData} />

          {/* 3. Leaderboard Table & Pagination */}
          <TopDishesTableSection
            dishes={dishes}
            paginatedDishes={paginatedDishes}
            maxQty={maxQty}
            currentPage={currentPage}
            totalPages={totalPages}
            itemsPerPage={ITEMS_PER_PAGE}
            onPageChange={setCurrentPage}
          />
        </div>
      )}
    </div>
  );
};

export default TopDishesStats;
