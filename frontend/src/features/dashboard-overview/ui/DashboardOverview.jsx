import React, { useState } from 'react';
import { Activity, AlertCircle, RefreshCw } from 'lucide-react';
import { useTranslation } from '@shared/lib/i18n';
import useDashboard from '../model/useDashboard.js';
import AiExecutiveSummaryCard from './AiExecutiveSummaryCard.jsx';
import CategorySharesCard from './CategorySharesCard.jsx';
import DashboardKpiCards from './DashboardKpiCards.jsx';
import KitchenPrepSuggestionsCard from './KitchenPrepSuggestionsCard.jsx';
import RecentOrdersCard from './RecentOrdersCard.jsx';
import RevenueForecastCard from './RevenueForecastCard.jsx';
import TopSellingDishesCard from './TopSellingDishesCard.jsx';

const DashboardOverview = () => {
  const {
    businessToday,
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
  const { isEn, language } = useTranslation('dashboard');

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
          {isEn ? 'Updating business metrics...' : 'Đang cập nhật số liệu kinh doanh...'}
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
            {isEn ? 'Unable to load data' : 'Không thể tải dữ liệu'}
          </h3>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-6">
            {isEn
              ? 'An error occurred connecting to the server. Please check your network.'
              : 'Đã xảy ra lỗi khi kết nối với máy chủ. Vui lòng kiểm tra lại đường truyền mạng.'}
          </p>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-5 py-2.5 text-sm font-bold text-white hover:from-rose-700 hover:to-red-700 transition-all shadow-md shadow-rose-600/25 active:scale-95"
          >
            <RefreshCw size={16} />
            {isEn ? 'Retry' : 'Thử lại ngay'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-w-0 space-y-6 lg:space-y-8 animate-fade-in-up">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-[#1E293B]/20 dark:border-[#1E293B]">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              {isEn ? 'Business Overview' : 'Tổng quan kinh doanh'}
            </h1>
            <span className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-[#1E293B]/25 dark:border-[#1E293B]">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
              {isEn ? 'Live' : 'Trực tiếp'}
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isEn
              ? 'Track revenue, table performance and AI-powered business forecasts.'
              : 'Theo dõi doanh thu, hiệu suất vận hành bàn và dự báo kinh doanh thông minh.'}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleManualRefresh}
            disabled={isRefreshing}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white dark:bg-[#0F172A] border border-[#1E293B] text-xs font-bold text-slate-800 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 shadow-sm transition-all active:scale-95 disabled:opacity-60"
          >
            <RefreshCw size={14} className={isRefreshing ? 'animate-spin text-indigo-600' : ''} />
            <span>
              {isRefreshing
                ? isEn
                  ? 'Refreshing...'
                  : 'Đang cập nhật...'
                : isEn
                  ? 'Refresh'
                  : 'Làm mới số liệu'}
            </span>
          </button>
        </div>
      </div>

      {/* 1. 4 Stats KPI Cards */}
      <DashboardKpiCards
        todayRevenue={todayRevenue}
        todayOrders={todayOrders}
        tablesContext={tablesContext}
        avgOrderValue={avgOrderValue}
        isEn={isEn}
      />

      {/* 2. AI Executive Summary */}
      <AiExecutiveSummaryCard businessToday={businessToday} />

      {/* 3. AI Forecasting & Kitchen Prep */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <RevenueForecastCard
          revenueForecastData={revenueForecastData}
          isEn={isEn}
          language={language}
        />
        <KitchenPrepSuggestionsCard
          popularDishesForecast={popularDishesForecast}
          isEn={isEn}
          language={language}
        />
      </div>

      {/* 4. Category Shares, Top 5 Dishes & Recent Orders */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <CategorySharesCard categoryShares={categoryShares} isEn={isEn} />
        <TopSellingDishesCard topDishes={topDishes} isEn={isEn} />
        <RecentOrdersCard recentOrders={recentOrders} isEn={isEn} language={language} />
      </div>
    </div>
  );
};

export default DashboardOverview;
