import React, { useState, useEffect } from 'react';
import {
    PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer
} from 'recharts';
import {
    TrendingUp, ShoppingBag, UtensilsCrossed, Clock, Loader2
} from 'lucide-react';

import { tableService } from '../../../services/admin/tableService';
import { orderService } from '../../../services/admin/orderService';
import { fmtVND, fmtTime, fmtStatus } from '../../../utils/formatters';

const Dashboard = () => {
    const [loading, setLoading] = useState(true);

    // Kpi data
    const [todayRevenue, setTodayRevenue] = useState(0);
    const [todayOrders, setTodayOrders] = useState({ total: 0, completed: 0 });
    const [tablesContext, setTablesContext] = useState({ total: 0, occupied: 0 });

    // Charts data
    const [topDishes, setTopDishes] = useState([]);
    const [categoryShares, setCategoryShares] = useState([]);
    const [recentOrders, setRecentOrders] = useState([]);

    useEffect(() => {
        const fetchDashboardData = async () => {
            setLoading(true);
            try {
                // Configure Date Bounds using browser local timezone
                const todayStart = new Date();
                todayStart.setHours(0, 0, 0, 0);

                const todayEnd = new Date();
                todayEnd.setHours(23, 59, 59, 999);

                const past7Start = new Date();
                past7Start.setDate(todayStart.getDate() - 6);
                past7Start.setHours(0, 0, 0, 0);

                // 1. Fetch tables
                const tables = await tableService.getAll();
                const occupied = tables.filter(t => t.status !== 'AVAILABLE' && t.status !== 'Trống').length;
                setTablesContext({ total: tables.length, occupied });

                // 2. Fetch ALL Orders
                const allOrders = await orderService.getAllOrders();

                // Sort all by time desc
                allOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

                // 3. Filter orders for Today
                const ordersTodayList = allOrders.filter(o => {
                    const d = new Date(o.createdAt);
                    return d >= todayStart && d <= todayEnd;
                });

                // Calculate KPI for today
                let rvToday = 0;
                let completed = 0;

                ordersTodayList.forEach(o => {
                    if (o.status !== 'CANCELLED') rvToday += o.totalAmount || 0;
                    if (o.status === 'PAID') completed++;
                });

                setTodayRevenue(rvToday);
                setTodayOrders({ total: ordersTodayList.length, completed });
                setRecentOrders(ordersTodayList.slice(0, 5)); // top 5 today

                // 4. Filter orders for Past 7 Days (for Pie Chart & Top Dishes)
                const ordersLast7Days = allOrders.filter(o => {
                    const d = new Date(o.createdAt);
                    return d >= past7Start && d <= todayEnd;
                });

                const dishMap = {};
                const catMap = {};

                ordersLast7Days.forEach(order => {
                    if (order.status === 'CANCELLED') return;
                    if (order.orderItems) {
                        order.orderItems.forEach(item => {
                            const name = item.menuItem ? item.menuItem.name : (item.combo ? `Combo ${item.combo.name}` : 'Món không tên');
                            const qty = item.quantity || 0;
                            const price = item.unitPrice || 0;

                            if (!dishMap[name]) dishMap[name] = { name, quantity: 0, revenue: 0 };
                            dishMap[name].quantity += qty;
                            dishMap[name].revenue += (qty * price);

                            const catName = item.menuItem?.category?.name || (item.combo ? 'Combo' : 'Khác');
                            if (!catMap[catName]) catMap[catName] = 0;
                            catMap[catName] += (qty * price);
                        });
                    }
                });

                const topDishesArr = Object.values(dishMap)
                    .sort((a, b) => b.quantity - a.quantity)
                    .slice(0, 5);

                setTopDishes(topDishesArr);

                const pieData = Object.keys(catMap).map(k => ({
                    name: k,
                    value: catMap[k]
                }));
                setCategoryShares(pieData);

            } catch (error) {
                console.error("Lỗi khi tải dữ liệu Dashboard:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchDashboardData();
    }, []);

    const COLORS_PIE = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#f59e0b'];

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
                                {todayOrders.total > 0 ? fmtVND(todayRevenue / todayOrders.total) : fmtVND(0)}
                            </h3>
                        </div>
                        <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center text-purple-600">
                            <DollarSignIcon size={20} />
                        </div>
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
                                        <p className="text-xs text-slate-500">Đã bán: <span className="font-medium">{dish.quantity}</span></p>
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-sm font-bold text-slate-800">{fmtVND(dish.revenue)}</p>
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
                                    <p className="text-xs text-slate-500">{order.table ? `Bàn ${order.table.tableNumber}` : 'Mang đi'} • {fmtTime(order.createdAt)}</p>
                                </div>
                                <div className="text-right flex flex-col items-end gap-1">
                                    <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${fmtStatus('order', order.status).color}`}>
                                        {fmtStatus('order', order.status).label}
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

export default Dashboard;
