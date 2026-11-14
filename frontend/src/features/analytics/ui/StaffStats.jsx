import React, { useState, useEffect, useMemo } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  Legend,
} from 'recharts';
import { Loader2, Award, Users } from 'lucide-react';
import { analyticsService } from '@entities/analytics/api/analyticsService.js';
import StatsToolbar from '@shared/ui/StatsToolbar.jsx';

import { fmtVND } from '@shared/lib/formatters.js';
import useDateRangeFilter from '../model/useDateRangeFilter.js';

const COLORS = ['#6366F1', '#8B5CF6', '#10B981', '#06B6D4', '#F59E0B', '#94A3B8'];

const StaffStats = () => {
  const { dateRange, setDateRange, appliedDateRange, handleApplyFilters } = useDateRangeFilter();
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await analyticsService.getUsers(appliedDateRange.from, appliedDateRange.to);
        setEmployees(data.sort((a, b) => (b.revenue || 0) - (a.revenue || 0)));
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [appliedDateRange]);

  const pieData = useMemo(() => {
    if (!employees.length) return [];
    const top5 = employees.slice(0, 5);
    const othersRevenue = employees.slice(5).reduce((acc, curr) => acc + (curr.revenue || 0), 0);

    const data = top5.map((e) => ({
      name: e.fullName,
      value: e.revenue || 0,
    }));

    if (othersRevenue > 0) {
      data.push({ name: 'Khác (Còn lại)', value: othersRevenue });
    }

    return data;
  }, [employees]);

  const maxRev = employees.length > 0 ? Math.max(...employees.map((e) => e.revenue || 0)) : 1;

  return (
    <div className="w-full min-w-0 space-y-6 p-0 sm:p-3 lg:p-6">
      <StatsToolbar
        dateRange={dateRange}
        setDateRange={setDateRange}
        onApply={handleApplyFilters}
        title="Thời gian"
      />

      {loading ? (
        <div className="p-20 text-center">
          <Loader2 className="animate-spin inline text-indigo-600 dark:text-indigo-400" size={36} />
        </div>
      ) : (
        <div className="flex flex-col lg:flex-row gap-6 mt-6">
          {/* PHẦN TRÁI: BIỂU ĐỒ TRÒN */}
          <div className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm lg:sticky lg:top-6 lg:w-1/3 dark:border-[#1E293B] dark:bg-[#0F172A]/90">
            <h3 className="font-bold text-slate-900 mb-4 dark:text-white">Tỷ lệ doanh thu</h3>
            <div className="h-[300px] w-full relative">
              {pieData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      innerRadius={65}
                      outerRadius={88}
                      paddingAngle={4}
                      dataKey="value"
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <RechartsTooltip
                      formatter={(value) => [fmtVND(value), 'Doanh thu']}
                      contentStyle={{
                        borderRadius: '16px',
                        border: '1px solid #1E293B',
                        boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)',
                      }}
                    />
                    <Legend verticalAlign="bottom" height={36} iconType="circle" />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex flex-col items-center justify-center h-full text-slate-400 dark:text-slate-500">
                  <Users size={48} className="opacity-20 mb-2" />
                  <p className="text-xs italic">Chưa có dữ liệu</p>
                </div>
              )}

              {/* Tổng doanh thu ở giữa biểu đồ */}
              {pieData.length > 0 && (
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none pb-8">
                  <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 dark:text-slate-500">
                    Tổng
                  </span>
                  <span className="text-xs font-black font-mono text-slate-900 dark:text-white">
                    {fmtVND(pieData.reduce((a, b) => a + b.value, 0))}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* PHẦN PHẢI: DANH SÁCH CHI TIẾT */}
          <div className="min-w-0 lg:w-2/3">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {employees.map((emp, idx) => (
                <div
                  key={`staff-${emp.fullName || idx}`}
                  className="rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 relative overflow-hidden group"
                >
                  {/* Huy hiệu Top 1-3 */}
                  {idx < 3 && (
                    <div
                      className={`absolute top-0 right-0 text-[10px] font-bold px-3 py-1 rounded-bl-2xl flex items-center gap-1 uppercase tracking-wider
                        ${
                          idx === 0
                            ? 'bg-amber-50 text-amber-700 border-b border-l border-amber-200/60 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800/40'
                            : idx === 1
                              ? 'bg-indigo-50 text-indigo-700 border-b border-l border-indigo-200/60 dark:bg-indigo-950/40 dark:text-indigo-300 dark:border-indigo-800/40'
                              : 'bg-teal-50 text-teal-700 border-b border-l border-teal-200/60 dark:bg-teal-950/40 dark:text-teal-300 dark:border-teal-800/40'
                        }`}
                    >
                      <Award size={13} /> Top {idx + 1}
                    </div>
                  )}

                  <div className="flex items-center gap-3.5 mb-4">
                    {emp.avatarUrl ? (
                      <img
                        src={emp.avatarUrl}
                        alt={emp.fullName}
                        className="w-12 h-12 rounded-full object-cover shadow-sm ring-2 ring-indigo-500/20"
                      />
                    ) : (
                      <div className="w-12 h-12 rounded-full flex items-center justify-center text-sm font-bold shadow-sm bg-gradient-to-tr from-indigo-600 to-violet-500 text-white">
                        {emp.fullName ? emp.fullName.charAt(0).toUpperCase() : '?'}
                      </div>
                    )}
                    <div className="min-w-0 flex-1">
                      <h4 className="font-bold text-slate-900 text-sm truncate dark:text-white">
                        {emp.fullName}
                      </h4>
                      <p className="text-xs font-semibold font-mono text-slate-400 dark:text-slate-500">
                        {emp.orders} đơn hàng
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2.5">
                    <div className="flex justify-between text-xs items-end">
                      <span className="text-slate-400 dark:text-slate-500 font-medium">Doanh thu</span>
                      <span className="font-black font-mono text-slate-900 text-base dark:text-white">
                        {fmtVND(emp.revenue)}
                      </span>
                    </div>

                    <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden dark:bg-slate-800">
                      <div
                        className="h-2 rounded-full transition-all duration-1000 ease-out"
                        style={{
                          width: `${(emp.revenue / maxRev) * 100}%`,
                          backgroundColor: idx < 5 ? COLORS[idx] : '#94A3B8',
                        }}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {employees.length === 0 && (
              <div className="rounded-3xl border border-dashed border-slate-200/80 bg-white p-12 text-center text-slate-400 dark:border-slate-800/80 dark:bg-slate-900/90 dark:text-slate-500 italic">
                <Users className="w-12 h-12 mx-auto mb-2 opacity-30" />
                <p>Không có dữ liệu nhân viên trong khoảng thời gian này</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default StaffStats;
