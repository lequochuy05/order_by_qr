import React from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ArrowDownRight,
  ArrowUpRight,
  Lightbulb,
  Sparkles,
  TrendingUp,
} from 'lucide-react';
import { fmtCompactVND, fmtVND } from '@shared/lib/formatters.js';
import {
  getForecastSummary,
  getRevenueInsight,
  getTrendLabel,
} from '../lib/forecastUtils.js';

const CustomForecastTooltip = ({ active, payload, label, language = 'vi' }) => {
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
                  {isActual
                    ? language === 'en'
                      ? 'Actual revenue'
                      : 'Doanh thu thực tế'
                    : language === 'en'
                      ? 'AI forecast'
                      : 'Dự báo AI'}
                  :
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

const RevenueForecastCard = ({ revenueForecastData, isEn, language }) => {
  const forecastSummary = getForecastSummary(revenueForecastData);
  const revenueInsight = getRevenueInsight(forecastSummary, language);

  return (
    <div className="min-w-0 lg:col-span-2 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
              <TrendingUp size={20} />
            </span>
            <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
              {isEn ? '7-day Revenue Forecast' : 'Dự báo doanh thu 7 ngày tới'}
            </h3>
          </div>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {isEn
              ? 'Combining actual sales data with AI cycle forecasting models.'
              : 'Kết hợp dữ liệu bán hàng thực tế và mô hình AI dự báo chu kỳ'}
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
              {isEn ? 'Tomorrow forecast' : 'Dự báo ngày mai'}
            </p>
            <p className="mt-1 text-lg font-black text-indigo-600 dark:text-indigo-400 font-mono">
              {fmtVND(forecastSummary.forecastTomorrow)}
            </p>
          </div>

          <div className="rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-[#1E293B]/25 dark:border-[#1E293B] p-4">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
              {isEn ? 'vs 7-day avg' : 'So với TB 7 ngày'}
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
              {isEn ? 'Trend' : 'Xu hướng'}
            </p>
            <p className="mt-1 text-lg font-black text-emerald-600 dark:text-emerald-400 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              {getTrendLabel(forecastSummary.trend, language)}
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
              {isEn ? 'Management tip:' : 'Gợi ý điều hành:'}{' '}
            </span>
            <span>{revenueInsight}</span>
          </div>
        </div>
      )}

      {/* Legend */}
      <div className="flex items-center gap-6 text-xs font-semibold text-slate-500 dark:text-slate-400 mb-4">
        <span className="flex items-center gap-2">
          <span className="w-3.5 h-3.5 rounded-md bg-indigo-500" />
          {isEn ? 'Actual revenue' : 'Doanh thu thực tế'}
        </span>
        <span className="flex items-center gap-2">
          <span className="w-3.5 h-1 border-t-2 border-dashed border-amber-500" />
          {isEn ? 'AI forecast' : 'Dự báo bởi AI'}
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
              <Tooltip content={<CustomForecastTooltip language={language} />} />
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
            {isEn ? 'No forecast data yet' : 'Chưa có dữ liệu dự báo doanh thu'}
          </div>
        )}
      </div>
    </div>
  );
};

export default RevenueForecastCard;
