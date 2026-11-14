import React, { useState, useEffect, useMemo } from 'react';
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
} from 'recharts';
import {
  UtensilsCrossed,
  ShoppingBag,
  TrendingUp,
  BarChart3,
  Award,
  ChevronLeft,
  ChevronRight,
  Flame,
  Activity,
} from 'lucide-react';
import { analyticsService } from '@entities/analytics/api/analyticsService.js';
import StatsToolbar from '@shared/ui/StatsToolbar.jsx';
import { fmtVND, fmtDate } from '@shared/lib/formatters.js';
import { formatBusinessDate } from '@shared/lib/businessTime.js';
import useDateRangeFilter from '../model/useDateRangeFilter.js';

const ITEMS_PER_PAGE = 10;

// Custom Tooltip for Horizontal Bar Chart
const CustomBarTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="rounded-2xl border border-slate-200/80 bg-white/95 p-3.5 shadow-xl backdrop-blur-md dark:border-slate-700/80 dark:bg-slate-900/95">
        <p className="text-xs font-bold text-slate-900 dark:text-white mb-2">{data.fullName}</p>
        <div className="space-y-1 text-xs">
          <div className="flex items-center justify-between gap-4">
            <span className="text-slate-500 dark:text-slate-400">Số lượng bán:</span>
            <span className="font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">
              {data.quantity.toLocaleString()} suất
            </span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span className="text-slate-500 dark:text-slate-400">Doanh thu:</span>
            <span className="font-extrabold text-slate-900 dark:text-slate-100 font-mono">
              {fmtVND(data.revenue)}
            </span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

// Custom Tooltip for Trend Area Chart
const CustomTrendTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const data = payload[0];
    return (
      <div className="rounded-2xl border border-slate-200/80 bg-white/95 px-3.5 py-2.5 shadow-xl backdrop-blur-md dark:border-slate-700/80 dark:bg-slate-900/95">
        <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1">
          {data.payload?.date || label}
        </p>
        <div className="flex items-center gap-2 text-xs">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
          <span className="text-slate-600 dark:text-slate-300 font-medium">Tổng bán ra:</span>
          <span className="font-bold text-slate-900 dark:text-white font-mono">
            {Number(data.value).toLocaleString()} suất
          </span>
        </div>
      </div>
    );
  }
  return null;
};

const TopDishesStats = () => {
  const { dateRange, setDateRange, appliedDateRange, handleApplyFilters: applyFilters } =
    useDateRangeFilter();
  const [dishes, setDishes] = useState([]);
  const [trend, setTrend] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const handleApplyFilters = () => applyFilters(() => setCurrentPage(0));

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [d, t] = await Promise.all([
          analyticsService.getTopDishes(appliedDateRange.from, appliedDateRange.to),
          analyticsService.getDishTrend(appliedDateRange.from, appliedDateRange.to),
        ]);
        setDishes(d);
        setTrend(t);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [appliedDateRange]);

  // KPI calculations
  const kpi = useMemo(() => {
    const totalDishes = dishes.length;
    const totalQty = dishes.reduce((s, d) => s + (d.totalQty || 0), 0);
    const totalRev = dishes.reduce((s, d) => s + (d.totalRevenue || 0), 0);
    const avg = totalDishes ? Math.round(totalQty / totalDishes) : 0;
    return { totalDishes, totalQty, totalRev, avg };
  }, [dishes]);

  // Bar chart data (Top 10 dishes)
  const barData = useMemo(() => {
    return dishes.slice(0, 10).map((d) => ({
      name: d.name.length > 16 ? d.name.substring(0, 16) + '...' : d.name,
      fullName: d.name,
      quantity: d.totalQty,
      revenue: d.totalRevenue,
    }));
  }, [dishes]);

  // Trend chart data
  const trendData = useMemo(() => {
    return trend.map((t) => ({
      date: fmtDate(t.bucket),
      quantity: t.totalQty,
    }));
  }, [trend]);

  // Max quantity for progress bars
  const maxQty = dishes.length > 0 ? dishes[0].totalQty : 1;

  const totalPages = Math.ceil(dishes.length / ITEMS_PER_PAGE);
  const paginatedDishes = useMemo(() => {
    const start = currentPage * ITEMS_PER_PAGE;
    return dishes.slice(start, start + ITEMS_PER_PAGE);
  }, [currentPage, dishes]);

  useEffect(() => {
    if (totalPages > 0 && currentPage >= totalPages) {
      setCurrentPage(totalPages - 1);
    }
  }, [currentPage, totalPages]);

  // Export CSV handler
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
    const content = ['﻿' + header, rows, totalRow].filter(Boolean).join('\n');
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
      {/* =========================================================================
          TOP BANNER / HEADER: Page Title & Description
          ========================================================================= */}
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
          {/* =========================================================================
              1. 4 STATS KPI CARDS: Evondev Style Glass Cards with Gradient Icons
              ========================================================================= */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
            {/* Card 1: Tổng số món đã bán */}
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

            {/* Card 2: Tổng sản lượng bán ra */}
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

            {/* Card 3: Doanh thu món ăn */}
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

            {/* Card 4: Trung bình SL / Món */}
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

          {/* =========================================================================
              2. 2 CHARTS: Horizontal BarChart (Top 10) & AreaChart (Trend by Day)
              ========================================================================= */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Horizontal BarChart - Top 10 Best Sellers */}
            <div className="min-w-0 rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm">
              <div className="flex items-center gap-2 mb-1">
                <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
                  <BarChart3 size={20} />
                </span>
                <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
                  Top 10 món bán chạy nhất
                </h3>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
                Xếp hạng 10 món ăn có sản lượng phục vụ cao nhất trong kỳ
              </p>

              <div className="h-[380px] w-full">
                {barData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={barData} layout="vertical" margin={{ left: 5, right: 20 }}>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        horizontal={false}
                        stroke="#E2E8F0"
                        opacity={0.5}
                      />
                      <XAxis
                        type="number"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748B', fontSize: 11 }}
                      />
                      <YAxis
                        type="category"
                        dataKey="name"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#334155', fontSize: 12, fontWeight: 600 }}
                        width={125}
                      />
                      <RechartsTooltip content={<CustomBarTooltip />} cursor={{ fill: 'rgba(99, 102, 241, 0.05)' }} />
                      <Bar
                        dataKey="quantity"
                        fill="#6366F1"
                        radius={[0, 8, 8, 0]}
                        barSize={20}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState />
                )}
              </div>
            </div>

            {/* AreaChart - Daily Trend */}
            <div className="min-w-0 rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm">
              <div className="flex items-center gap-2 mb-1">
                <span className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
                  <TrendingUp size={20} />
                </span>
                <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
                  Xu hướng gọi món theo ngày
                </h3>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
                Tổng số suất ăn phục vụ qua từng mốc thời gian
              </p>

              <div className="h-[380px] w-full">
                {trendData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={trendData} margin={{ top: 10, right: 10, bottom: 5, left: 0 }}>
                      <defs>
                        <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                          <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        vertical={false}
                        stroke="#E2E8F0"
                        opacity={0.5}
                      />
                      <XAxis
                        dataKey="date"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748B', fontSize: 11 }}
                        dy={6}
                      />
                      <YAxis
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748B', fontSize: 11 }}
                        allowDecimals={false}
                        width={40}
                      />
                      <RechartsTooltip content={<CustomTrendTooltip />} />
                      <Area
                        type="monotone"
                        dataKey="quantity"
                        stroke="#10B981"
                        fill="url(#trendGrad)"
                        strokeWidth={3}
                        activeDot={{ r: 6, fill: '#10B981', stroke: '#fff', strokeWidth: 2 }}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState />
                )}
              </div>
            </div>
          </div>

          {/* =========================================================================
              3. LEADERBOARD TABLE: Full Dishes Ranking & Statistics
              ========================================================================= */}
          <div className="rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] shadow-sm overflow-hidden">
            {/* Table Header */}
            <div className="p-6 border-b border-[#1E293B]/20 dark:border-[#1E293B] flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
                  <Award size={20} />
                </span>
                <div>
                  <h3 className="font-extrabold text-lg text-slate-900 dark:text-white tracking-tight">
                    Bảng xếp hạng toàn bộ món ăn
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Danh sách tổng hợp sản lượng và doanh thu từng món
                  </p>
                </div>
              </div>
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                {dishes.length} món
              </span>
            </div>

            {/* Table View */}
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-950/50 text-slate-500 dark:text-slate-400 text-xs uppercase font-bold tracking-wider">
                    <th className="p-4 w-16 text-center">#</th>
                    <th className="p-4 w-16">Hình</th>
                    <th className="p-4">Tên món</th>
                    <th className="p-4">Danh mục</th>
                    <th className="p-4 text-center">SL đã bán</th>
                    <th className="p-4 text-right">Doanh thu</th>
                    <th className="p-4 w-44 pr-6">Tỉ trọng bán</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                  {paginatedDishes.map((dish, idx) => {
                    const rank = currentPage * ITEMS_PER_PAGE + idx + 1;
                    const percentOfTop = Math.min(
                      100,
                      Math.round((dish.totalQty / maxQty) * 100),
                    );

                    // Rank badge style
                    const rankColors = [
                      'bg-gradient-to-r from-amber-400 to-amber-600 text-white shadow-md shadow-amber-500/25',
                      'bg-gradient-to-r from-slate-400 to-slate-600 text-white shadow-md shadow-slate-500/25',
                      'bg-gradient-to-r from-amber-700 to-amber-900 text-white shadow-md shadow-amber-800/25',
                    ];
                    const rankBadge =
                      rank <= 3 ? (
                        <span
                          className={`inline-flex items-center justify-center w-8 h-8 rounded-xl text-xs font-black ${rankColors[rank - 1]}`}
                        >
                          {rank}
                        </span>
                      ) : (
                        <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 text-xs font-bold">
                          {rank}
                        </span>
                      );

                    return (
                      <tr
                        key={`dish-${dish.menuItemId || rank}`}
                        className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors"
                      >
                        {/* Rank */}
                        <td className="p-4 text-center">{rankBadge}</td>

                        {/* Thumbnail */}
                        <td className="p-4">
                          {dish.img ? (
                            <img
                              src={dish.img}
                              alt={dish.name}
                              className="w-11 h-11 rounded-xl object-cover border border-slate-200/80 dark:border-slate-700 shadow-sm"
                            />
                          ) : (
                            <div className="w-11 h-11 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200/60 dark:border-slate-700 flex items-center justify-center text-slate-400">
                              <UtensilsCrossed size={18} />
                            </div>
                          )}
                        </td>

                        {/* Dish Name */}
                        <td className="p-4">
                          <p
                            className="font-bold text-slate-900 dark:text-white line-clamp-1"
                            title={dish.name}
                          >
                            {dish.name}
                          </p>
                        </td>

                        {/* Category */}
                        <td className="p-4">
                          <span className="inline-flex px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-400 border border-indigo-100/60 dark:border-indigo-900/40">
                            {dish.categoryName || dish.category || 'Khác'}
                          </span>
                        </td>

                        {/* Quantity Sold */}
                        <td className="p-4 text-center">
                          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-black font-mono bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-400">
                            {dish.totalQty}
                          </span>
                        </td>

                        {/* Revenue */}
                        <td className="p-4 text-right font-black font-mono text-slate-900 dark:text-white">
                          {fmtVND(dish.totalRevenue)}
                        </td>

                        {/* Progress Bar (Share of top seller) */}
                        <td className="p-4 pr-6">
                          <div className="w-full flex items-center gap-2">
                            <div className="flex-1 bg-slate-100 dark:bg-slate-800 rounded-full h-2 overflow-hidden">
                              <div
                                className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-700 ease-out"
                                style={{ width: `${percentOfTop}%` }}
                              />
                            </div>
                            <span className="text-[10px] font-bold text-slate-400 font-mono w-8 text-right">
                              {percentOfTop}%
                            </span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}

                  {dishes.length === 0 && (
                    <tr key="empty-row">
                      <td colSpan="7" className="p-12 text-center text-slate-400 dark:text-slate-500">
                        <UtensilsCrossed className="w-12 h-12 mx-auto mb-3 opacity-20" />
                        <p className="font-semibold text-sm">
                          Không có dữ liệu món ăn trong khoảng thời gian đã chọn.
                        </p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex flex-col sm:flex-row justify-between items-center gap-3 p-4 px-6 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                  Hiển thị{' '}
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {currentPage * ITEMS_PER_PAGE + 1}
                  </span>{' '}
                  -{' '}
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {Math.min((currentPage + 1) * ITEMS_PER_PAGE, dishes.length)}
                  </span>{' '}
                  trong tổng số{' '}
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {dishes.length}
                  </span>{' '}
                  món
                </span>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={currentPage === 0}
                    onClick={() => setCurrentPage((prev) => prev - 1)}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/80 disabled:opacity-40 transition-all shadow-sm"
                  >
                    <ChevronLeft size={14} />
                    <span>Trước</span>
                  </button>

                  <span className="px-3 py-1 rounded-lg text-xs font-bold text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                    {currentPage + 1} / {totalPages}
                  </span>

                  <button
                    type="button"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setCurrentPage((prev) => prev + 1)}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/80 disabled:opacity-40 transition-all shadow-sm"
                  >
                    <span>Sau</span>
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const EmptyState = () => (
  <div className="h-full flex flex-col items-center justify-center text-slate-400 dark:text-slate-500 py-12">
    <UtensilsCrossed size={48} className="opacity-20 mb-3" />
    <p className="text-sm font-semibold">Chưa có dữ liệu thống kê món ăn</p>
  </div>
);

export default TopDishesStats;
