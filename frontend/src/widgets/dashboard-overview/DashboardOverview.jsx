import React, { useState, useEffect, useMemo } from 'react';
import {
    PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer,
    LineChart, Line, XAxis, YAxis, CartesianGrid
} from 'recharts';
import {
    TrendingUp, ShoppingBag, UtensilsCrossed, Clock, Loader2, Sparkles
} from 'lucide-react';

import { tableService } from '@modules/table-management/api/tableService.js';
import { statisticsService } from '@modules/statistics/api/statisticsService.js';
import { orderService } from '@modules/order-management/api/orderService.js';
import { fmtVND, fmtTime } from '@shared/lib/formatters.js';
import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { addDaysToBusinessDate, getBusinessToday } from '@shared/lib/businessTime.js';

const Dashboard = () => {
    const [loading, setLoading] = useState(true);
    const [businessToday, setBusinessToday] = useState(getBusinessToday);

    // Kpi data
    const [todayRevenue, setTodayRevenue] = useState(0);
    const [todayOrders, setTodayOrders] = useState({ total: 0, completed: 0 });
    const [tablesContext, setTablesContext] = useState({ total: 0, occupied: 0 });

    // Charts data
    const [topDishes, setTopDishes] = useState([]);
    const [categoryShares, setCategoryShares] = useState([]);
    const [recentOrders, setRecentOrders] = useState([]);
    const [revenueForecast, setRevenueForecast] = useState([]);
    const [popularDishesForecast, setPopularDishesForecast] = useState([]);

    const revenueForecastData = useMemo(
        () => buildRevenueForecastData(revenueForecast),
        [revenueForecast]
    );

    useEffect(() => {
        const syncBusinessDate = () => {
            setBusinessToday(currentDate => {
                const nextDate = getBusinessToday();
                return currentDate === nextDate ? currentDate : nextDate;
            });
        };

        const intervalId = window.setInterval(syncBusinessDate, 30_000);
        document.addEventListener('visibilitychange', syncBusinessDate);

        return () => {
            window.clearInterval(intervalId);
            document.removeEventListener('visibilitychange', syncBusinessDate);
        };
    }, []);

    useEffect(() => {
        const fetchDashboardData = async () => {
            setLoading(true);
            try {
                const past7Start = addDaysToBusinessDate(businessToday, -6);

                // Fetch all data in parallel using server-side filtered APIs
                const [
                    tables,
                    todayRevenueData,
                    todayOrderStatsData,
                    completedOrderStatsData,
                    recentOrdersData,
                    topDishesData,
                    revenueForecastResult,
                    dishForecastResult
                ] = await Promise.allSettled([
                    tableService.getAll(),
                    statisticsService.getRevenue(businessToday, businessToday),
                    orderService.getOrderStats({ startDate: businessToday, endDate: businessToday }),
                    orderService.getOrderStats({ status: 'COMPLETED', startDate: businessToday, endDate: businessToday }),
                    orderService.getOrderHistory({ startDate: businessToday, endDate: businessToday, page: 0, size: 5 }),
                    statisticsService.getTopDishes(past7Start, businessToday),
                    statisticsService.getRevenueForecast(),
                    statisticsService.getPopularDishesForecast()
                ]);

                // 1. Tables
                if (tables.status === 'fulfilled') {
                    const tableData = tables.value;
                    const occupied = tableData.filter(t => t.status !== 'AVAILABLE' && t.status !== 'Trống').length;
                    setTablesContext({ total: tableData.length, occupied });
                }

                // 2. Today Revenue (from server)
                if (todayRevenueData.status === 'fulfilled') {
                    const revenueArr = todayRevenueData.value;
                    const rvToday = Array.isArray(revenueArr)
                        ? revenueArr.reduce((sum, r) => sum + (r.revenue || r.totalRevenue || 0), 0)
                        : (revenueArr?.totalRevenue || revenueArr?.revenue || 0);
                    setTodayRevenue(rvToday);
                }

                // 3. Today Orders: count all orders created today, not just paid/completed ones.
                if (todayOrderStatsData.status === 'fulfilled') {
                    setTodayOrders(current => ({
                        ...current,
                        total: Number(todayOrderStatsData.value?.totalOrders || 0)
                    }));
                }

                if (completedOrderStatsData.status === 'fulfilled') {
                    setTodayOrders(current => ({
                        ...current,
                        completed: Number(completedOrderStatsData.value?.totalOrders || 0)
                    }));
                }

                if (recentOrdersData.status === 'fulfilled') {
                    const ordersPage = recentOrdersData.value;
                    setRecentOrders(Array.isArray(ordersPage?.content) ? ordersPage.content : []);
                }

                // 4. Top Dishes & 5. Category shares (server-side aggregation)
                if (topDishesData.status === 'fulfilled') {
                    const dishes = topDishesData.value;
                    setTopDishes(Array.isArray(dishes) ? dishes.slice(0, 5) : []);

                    if (Array.isArray(dishes)) {
                        const catMap = {};
                        dishes.forEach(dish => {
                            const catName = dish.category || 'Khác';
                            if (!catMap[catName]) catMap[catName] = 0;
                            catMap[catName] += (dish.totalRevenue || 0);
                        });
                        const pieData = Object.keys(catMap).map(k => ({ name: k, value: catMap[k] }));
                        setCategoryShares(pieData);
                    }
                }

                // 6. Forecasts
                if (revenueForecastResult.status === 'fulfilled') {
                    setRevenueForecast(revenueForecastResult.value);
                } else {
                    setRevenueForecast([]);
                }

                if (dishForecastResult.status === 'fulfilled') {
                    setPopularDishesForecast(dishForecastResult.value);
                } else {
                    setPopularDishesForecast([]);
                }

            } catch (error) {
                console.error("Lỗi khi tải dữ liệu Dashboard:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchDashboardData();
    }, [businessToday]);

    const COLORS_PIE = ['#f97316', '#3b82f6', '#10b981', '#14b8a6', '#ec4899', '#f59e0b'];

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen bg-slate-50">
                <Loader2 className="animate-spin text-orange-500 w-12 h-12" />
            </div>
        );
    }

    return (
        <div className="p-6 bg-slate-50 min-h-screen">

            {/* 1. KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-6">

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
                            <p className="text-slate-500 text-sm font-medium mb-1">Bàn hoạt động</p>
                            <h3 className="text-2xl font-bold text-slate-800">
                                {tablesContext.occupied} <span className="text-lg text-slate-400 font-medium">/ {tablesContext.total}</span>
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

                {/* Avg Order Value / Or other metric */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow">
                    <div className="flex justify-between items-start">
                        <div>
                            <p className="text-slate-500 text-sm font-medium mb-1">Giá trị trung bình đơn</p>
                            <h3 className="text-2xl font-bold text-slate-800">
                                {todayOrders.completed > 0 ? fmtVND(todayRevenue / todayOrders.completed) : fmtVND(0)}
                            </h3>
                        </div>
                        <div className="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center text-teal-600">
                            <DollarSignIcon size={20} />
                        </div>
                    </div>
                </div>

            </div>

            {/* Forecasting Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
                <div className="lg:col-span-2 bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <TrendingUp size={18} className="text-orange-500" /> Dự báo doanh thu
                        </h3>
                        <div className="flex items-center gap-4 text-xs font-medium text-slate-500">
                            <span className="flex items-center gap-2"><span className="w-3 h-0.5 bg-emerald-500"></span> Thực tế</span>
                            <span className="flex items-center gap-2"><span className="w-3 h-0.5 border-t-2 border-dashed border-amber-500"></span> Dự báo</span>
                        </div>
                    </div>
                    <div className="h-80">
                        {revenueForecastData.length > 0 ? (
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={revenueForecastData} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                    <XAxis
                                        dataKey="label"
                                        tick={{ fontSize: 12, fill: '#64748b' }}
                                        minTickGap={18}
                                        axisLine={false}
                                        tickLine={false}
                                    />
                                    <YAxis
                                        tickFormatter={formatCompactVND}
                                        tick={{ fontSize: 12, fill: '#64748b' }}
                                        axisLine={false}
                                        tickLine={false}
                                        width={72}
                                    />
                                    <Tooltip
                                        formatter={(value, name) => [fmtVND(value), name === 'actual' ? 'Thực tế' : 'Dự báo']}
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
                            <div className="h-full flex items-center justify-center text-slate-400">Chưa có dữ liệu dự báo doanh thu</div>
                        )}
                    </div>
                </div>

                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
                        <Sparkles size={18} className="text-amber-500" /> Món dự báo tuần tới
                    </h3>
                    <div className="flex flex-col gap-4">
                        {popularDishesForecast.map((dish, idx) => (
                            <div key={dish.id || idx} className="flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent shadow-sm">
                                <div className="flex items-center gap-4 min-w-0">
                                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold text-white shadow-sm shrink-0 ${idx === 0 ? 'bg-amber-500' : idx === 1 ? 'bg-orange-500' : idx === 2 ? 'bg-emerald-500' : 'bg-slate-300'}`}>
                                        {idx + 1}
                                    </div>
                                    <div className="min-w-0">
                                        <p className="font-semibold text-slate-800 truncate" title={dish.name}>
                                            {dish.name}
                                        </p>
                                        <p className="text-xs text-slate-500 truncate">{dish.category || 'Chưa phân loại'}</p>
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-sm font-bold text-slate-800">{dish.estimatedQty}</p>
                                    <p className="text-xs text-slate-500">suất</p>
                                </div>
                            </div>
                        ))}
                        {popularDishesForecast.length === 0 && <p className="text-center text-slate-400 py-4">Chưa có dữ liệu dự báo món.</p>}
                    </div>
                </div>
            </div>

            {/* 2 & 4. Charts and Leaderboard Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">

                {/* Tỉ trọng danh mục (Pie Chart) - Nắm 1 cột */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800 mb-6">Tỉ trọng Danh mục (7 ngày)</h3>
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
                            <div className="h-full flex items-center justify-center text-slate-400">Chưa có thông tin bán hàng</div>
                        )}
                    </div>
                </div>

                {/* Top 5 Món Bán Chạy (Leaderboard) - Nắm 1 cột */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
                        <UtensilsCrossed size={18} className="text-emerald-500" /> Top món (7 ngày)
                    </h3>
                    <div className="flex flex-col gap-4">
                        {topDishes.map((dish, idx) => (
                            <div key={idx} className="flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent shadow-sm">
                                <div className="flex items-center gap-4">
                                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold text-white shadow-sm ${idx === 0 ? 'bg-orange-500' : idx === 1 ? 'bg-blue-500' : idx === 2 ? 'bg-emerald-500' : 'bg-slate-300'}`}>
                                        {idx + 1}
                                    </div>
                                    <div>
                                        <p className="font-semibold text-slate-800 line-clamp-1" title={dish.name}>
                                            {dish.name.length > 18 ? dish.name.substring(0, 18) + '...' : dish.name}
                                        </p>
                                        <p className="text-xs text-slate-500">Đã bán: <span className="font-medium">{dish.totalQty}</span></p>
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-sm font-bold text-slate-800">{fmtVND(dish.totalRevenue)}</p>
                                </div>
                            </div>
                        ))}
                        {topDishes.length === 0 && <p className="text-center text-slate-400 py-4">Chưa có món nào được bán ra.</p>}
                    </div>
                </div>

                {/* Giao dịch gần nhất hôm nay (Recent Orders Activity) - Nắm 1 cột */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
                        <Clock size={18} className="text-blue-500" /> Đơn hàng mới nhất
                    </h3>
                    <div className="flex flex-col gap-4">
                        {recentOrders.map((order, idx) => (
                            <div key={idx} className="flex justify-between items-start p-3 border-b border-slate-50 last:border-0 hover:bg-slate-50 rounded-xl transition-colors">
                                <div>
                                    <p className="text-sm font-semibold text-slate-700">Đơn #{order.id}</p>
                                    <p className="text-xs text-slate-500">{order.table?.tableNumber ? `Bàn ${order.table.tableNumber}` : 'Mang đi'} • {fmtTime(order.paymentTime || order.createdAt)}</p>
                                </div>
                                <div className="text-right flex flex-col items-end gap-1">
                                    <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${getOrderStatusMeta(order.status).classes}`}>
                                        {getOrderStatusMeta(order.status).label}
                                    </span>
                                    <span className="text-sm font-bold text-slate-800">{fmtVND(order.totalAmount)}</span>
                                </div>
                            </div>
                        ))}
                        {recentOrders.length === 0 && <p className="text-center text-slate-400 py-4">Hôm nay chưa có đơn hàng nào.</p>}
                    </div>
                </div>

            </div>
        </div>
    );
};

const DollarSignIcon = ({ size }) => (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-banknote"><rect width="20" height="12" x="2" y="6" rx="2" /><circle cx="12" cy="12" r="2" /><path d="M6 12h.01M18 12h.01" /></svg>
);

const buildRevenueForecastData = (points) => {
    const rows = points.map((point) => ({
        date: point.date,
        label: formatChartDate(point.date),
        actual: point.actual == null ? null : Number(point.actual),
        forecast: point.forecast == null ? null : Number(point.forecast),
        forecasted: point.forecasted
    }));

    const firstForecastIndex = rows.findIndex((row) => row.forecasted);
    if (firstForecastIndex > 0 && rows[firstForecastIndex - 1].actual != null) {
        rows[firstForecastIndex - 1] = {
            ...rows[firstForecastIndex - 1],
            forecast: rows[firstForecastIndex - 1].actual
        };
    }

    return rows;
};

const formatChartDate = (date) => {
    if (!date) return '';
    const [, month, day] = date.split('-');
    return `${day}/${month}`;
};

const formatCompactVND = (value) => {
    if (value >= 1000000) return `${Math.round(value / 1000000)}tr`;
    if (value >= 1000) return `${Math.round(value / 1000)}k`;
    return value;
};

export default Dashboard;
