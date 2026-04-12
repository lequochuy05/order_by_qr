import React, { useState, useEffect, useMemo } from 'react';
import {
    BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid,
    ResponsiveContainer, Tooltip as RechartsTooltip
} from 'recharts';
import { Loader2, UtensilsCrossed, ShoppingBag, TrendingUp, BarChart3, Award, Download } from 'lucide-react';
import { statisticsService } from '../../../services/admin/statisticsService';
import StatsToolbar from '../../../components/admin/common/StatsToolbar';
import { fmtVND, fmtDate } from '../../../utils/formatters';

const TopDishesStats = () => {
    const [dateRange, setDateRange] = useState({
        from: new Date(new Date().setDate(new Date().getDate() - 6)),
        to: new Date()
    });
    const [dishes, setDishes] = useState([]);
    const [trend, setTrend] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [d, t] = await Promise.all([
                    statisticsService.getTopDishes(dateRange.from, dateRange.to),
                    statisticsService.getDishTrend(dateRange.from, dateRange.to)
                ]);
                setDishes(d);
                setTrend(t);
            } catch (e) { console.error(e); }
            finally { setLoading(false); }
        };
        load();
    }, [dateRange]);

    // KPI
    const kpi = useMemo(() => {
        const totalDishes = dishes.length;
        const totalQty = dishes.reduce((s, d) => s + (d.totalQuantity || 0), 0);
        const totalRev = dishes.reduce((s, d) => s + (d.totalRevenue || 0), 0);
        const avg = totalDishes ? Math.round(totalQty / totalDishes) : 0;
        return { totalDishes, totalQty, totalRev, avg };
    }, [dishes]);

    // Bar chart data (top 10, reversed for horizontal display)
    const barData = useMemo(() => {
        return dishes.slice(0, 10).map(d => ({
            name: d.name.length > 16 ? d.name.substring(0, 16) + '...' : d.name,
            fullName: d.name,
            quantity: d.totalQuantity,
            revenue: d.totalRevenue
        })).reverse();
    }, [dishes]);

    // Trend chart data
    const trendData = useMemo(() => {
        return trend.map(t => ({
            date: fmtDate(new Date(t.date)),
            quantity: t.totalQuantity
        }));
    }, [trend]);

    // Max quantity for progress bars
    const maxQty = dishes.length > 0 ? dishes[0].totalQuantity : 1;

    // Export CSV
    const handleExport = () => {
        const header = 'Hạng,Tên món,Danh mục,SL bán,Doanh thu\n';
        const rows = dishes.map((d, i) =>
            `${i + 1},"${d.name}","${d.categoryName || ''}",${d.totalQuantity},${d.totalRevenue}`
        ).join('\n');
        const blob = new Blob(['\uFEFF' + header + rows], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `top-dishes-${dateRange.from.toISOString().split('T')[0]}_${dateRange.to.toISOString().split('T')[0]}.csv`;
        a.click();
        URL.revokeObjectURL(url);
    };

    const COLORS_TOP = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#f59e0b', '#06b6d4', '#e11d48', '#84cc16', '#6366f1'];

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <StatsToolbar dateRange={dateRange} setDateRange={setDateRange} title="Thời gian" onExport={handleExport} />

            {loading ? (
                <div className="p-20 text-center"><Loader2 className="animate-spin inline text-orange-500" size={32} /></div>
            ) : (
                <div className="space-y-6">

                    {/* 1. KPI Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                        <KpiCard title="Tổng số món đã bán" value={kpi.totalDishes} icon={<UtensilsCrossed size={20} />} color="orange" />
                        <KpiCard title="Tổng SL bán ra" value={kpi.totalQty.toLocaleString()} icon={<ShoppingBag size={20} />} color="blue" />
                        <KpiCard title="Tổng doanh thu món ăn" value={fmtVND(kpi.totalRev)} icon={<TrendingUp size={20} />} color="emerald" />
                        <KpiCard title="Trung bình SL/Món" value={kpi.avg} icon={<BarChart3 size={20} />} color="amber" />
                    </div>

                    {/* 2. Charts */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Horizontal BarChart - Top 10 */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border">
                            <h3 className="font-bold text-gray-800 mb-6 flex items-center gap-2">
                                <BarChart3 size={18} className="text-blue-500" /> Top 10 món bán chạy nhất
                            </h3>
                            <div className="h-[400px]">
                                {barData.length > 0 ? (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={barData} layout="vertical" margin={{ left: 10 }}>
                                            <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0f0f0" />
                                            <XAxis type="number" axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} />
                                            <YAxis type="category" dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#374151', fontSize: 12 }} width={130} />
                                            <RechartsTooltip
                                                cursor={{ fill: '#f3f4f6' }}
                                                formatter={(v, name) => [
                                                    name === 'quantity' ? `${v} phần` : fmtVND(v),
                                                    name === 'quantity' ? 'Số lượng' : 'Doanh thu'
                                                ]}
                                                labelFormatter={(l, p) => p[0]?.payload.fullName}
                                                contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                            />
                                            <Bar dataKey="quantity" fill="#3b82f6" radius={[0, 6, 6, 0]} barSize={24} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                ) : (
                                    <EmptyState />
                                )}
                            </div>
                        </div>

                        {/* AreaChart - Xu hướng bán theo ngày */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border">
                            <h3 className="font-bold text-gray-800 mb-6 flex items-center gap-2">
                                <TrendingUp size={18} className="text-orange-500" /> Xu hướng bán theo ngày
                            </h3>
                            <div className="h-[400px]">
                                {trendData.length > 0 ? (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={trendData}>
                                            <defs>
                                                <linearGradient id="colorQty" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#f97316" stopOpacity={0.2} />
                                                    <stop offset="95%" stopColor="#f97316" stopOpacity={0} />
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                                            <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} dy={10} />
                                            <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} allowDecimals={false} />
                                            <RechartsTooltip
                                                formatter={(v) => [`${v} phần`, 'Tổng SL bán']}
                                                contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                            />
                                            <Area type="monotone" dataKey="quantity" stroke="#f97316" fill="url(#colorQty)" strokeWidth={3} activeDot={{ r: 6 }} />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                ) : (
                                    <EmptyState />
                                )}
                            </div>
                        </div>
                    </div>

                    {/* 3. Bảng xếp hạng */}
                    <div className="bg-white rounded-3xl shadow-sm border overflow-hidden">
                        <div className="p-6 border-b border-gray-100 flex items-center justify-between">
                            <h3 className="font-bold text-gray-800 flex items-center gap-2">
                                <Award size={20} className="text-yellow-500" /> Bảng xếp hạng toàn bộ
                            </h3>
                            <span className="text-sm text-gray-500">{dishes.length} món</span>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead className="bg-gray-50 text-gray-500 text-xs uppercase font-semibold">
                                    <tr>
                                        <th className="p-4 w-16 text-center">#</th>
                                        <th className="p-4 w-16">Hình</th>
                                        <th className="p-4">Tên món</th>
                                        <th className="p-4">Danh mục</th>
                                        <th className="p-4 text-center">SL đã bán</th>
                                        <th className="p-4 text-right">Doanh thu</th>
                                        <th className="p-4 w-40"></th>
                                    </tr>
                                </thead>
                                <tbody className="text-sm">
                                    {dishes.map((dish, idx) => (
                                        <tr key={dish.menuItemId} className="border-b last:border-0 hover:bg-slate-50 transition-colors">
                                            {/* Rank */}
                                            <td className="p-4 text-center">
                                                {idx < 3 ? (
                                                    <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full text-white text-sm font-bold shadow-sm
                                                        ${idx === 0 ? 'bg-yellow-500' : idx === 1 ? 'bg-gray-400' : 'bg-orange-400'}`}>
                                                        {idx + 1}
                                                    </span>
                                                ) : (
                                                    <span className="text-gray-400 font-medium">{idx + 1}</span>
                                                )}
                                            </td>
                                            {/* Thumbnail */}
                                            <td className="p-4">
                                                {dish.img ? (
                                                    <img src={dish.img} alt={dish.name} className="w-10 h-10 rounded-lg object-cover shadow-sm" />
                                                ) : (
                                                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                                                        <UtensilsCrossed size={16} className="text-gray-300" />
                                                    </div>
                                                )}
                                            </td>
                                            {/* Name */}
                                            <td className="p-4">
                                                <p className="font-semibold text-gray-800 line-clamp-1" title={dish.name}>{dish.name}</p>
                                            </td>
                                            {/* Category */}
                                            <td className="p-4">
                                                <span className="px-2 py-1 bg-blue-50 text-blue-700 rounded-lg text-xs font-medium">
                                                    {dish.categoryName || 'Khác'}
                                                </span>
                                            </td>
                                            {/* Quantity */}
                                            <td className="p-4 text-center">
                                                <span className="px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-sm font-bold">
                                                    {dish.totalQuantity}
                                                </span>
                                            </td>
                                            {/* Revenue */}
                                            <td className="p-4 text-right font-bold text-gray-800">
                                                {fmtVND(dish.totalRevenue)}
                                            </td>
                                            {/* Progress bar */}
                                            <td className="p-4 pr-6">
                                                <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
                                                    <div
                                                        className="h-2 rounded-full transition-all duration-700 ease-out"
                                                        style={{
                                                            width: `${(dish.totalQuantity / maxQty) * 100}%`,
                                                            backgroundColor: COLORS_TOP[idx % COLORS_TOP.length]
                                                        }}
                                                    />
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    {dishes.length === 0 && (
                                        <tr>
                                            <td colSpan="7" className="p-8 text-center text-gray-400">
                                                <UtensilsCrossed className="w-12 h-12 mx-auto mb-2 opacity-20" />
                                                <p>Không có dữ liệu món ăn trong khoảng thời gian này.</p>
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {dishes.length > 0 && (
                            <div className="p-4 border-t border-gray-100 flex justify-center">
                                <button onClick={handleExport} className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 rounded-xl transition-colors">
                                    <Download size={16} /> Xuất CSV ({dishes.length} món)
                                </button>
                            </div>
                        )}
                    </div>

                </div>
            )}
        </div>
    );
};

// Sub-components
const KpiCard = ({ title, value, icon, color }) => {
    const colorMap = {
        orange: { bg: 'bg-orange-100', text: 'text-orange-600' },
        blue: { bg: 'bg-blue-100', text: 'text-blue-600' },
        emerald: { bg: 'bg-emerald-100', text: 'text-emerald-600' },
        amber: { bg: 'bg-amber-100', text: 'text-amber-600' },
    };
    const c = colorMap[color] || colorMap.orange;

    return (
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-100 flex justify-between items-center hover:shadow-md transition-shadow">
            <div>
                <p className="text-gray-500 text-sm font-medium">{title}</p>
                <h3 className={`text-2xl font-black mt-1 ${c.text}`}>{value}</h3>
            </div>
            <div className={`w-10 h-10 rounded-full ${c.bg} flex items-center justify-center ${c.text}`}>
                {icon}
            </div>
        </div>
    );
};

const EmptyState = () => (
    <div className="h-full flex flex-col items-center justify-center text-gray-400">
        <UtensilsCrossed size={48} className="opacity-20 mb-2" />
        <p>Chưa có dữ liệu</p>
    </div>
);

export default TopDishesStats;
