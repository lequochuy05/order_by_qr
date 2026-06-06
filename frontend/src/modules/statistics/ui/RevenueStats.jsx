import React, { useState, useEffect, useMemo } from 'react';
import {
    BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid,
    ResponsiveContainer, Tooltip as RechartsTooltip, Tooltip
} from 'recharts';
import { Loader2, Receipt } from 'lucide-react'; // Thêm icon Receipt
import { statisticsService } from '@modules/statistics/api/statisticsService.js';
import StatsToolbar from '@shared/ui/StatsToolbar.jsx';
import { fmtVND, fmtDate, fmtDateTime } from '@shared/lib/formatters.js';

const ITEMS_PER_PAGE = 10;

const getDefaultDateRange = () => {
    const to = new Date();
    const from = new Date(to);
    from.setDate(to.getDate() - 6);
    return { from, to };
};

const RevenueStats = () => {
    // Mặc định 7 ngày  
    const [dateRange, setDateRange] = useState(getDefaultDateRange);
    const [appliedDateRange, setAppliedDateRange] = useState(dateRange);
    const [revenueData, setRevenueData] = useState([]);
    const [orders, setOrders] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);

    const handleApplyFilters = () => {
        setCurrentPage(0);
        setAppliedDateRange({
            from: new Date(dateRange.from),
            to: new Date(dateRange.to)
        });
    };

    // Gọi API khi áp dụng khoảng ngày
    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [rev, ods] = await Promise.all([
                    statisticsService.getRevenue(appliedDateRange.from, appliedDateRange.to),
                    statisticsService.getOrders(appliedDateRange.from, appliedDateRange.to)
                ]);
                setRevenueData(rev);
                // Sắp xếp đơn hàng mới nhất lên đầu để hiển thị trong bảng
                setOrders(ods.sort((a, b) => new Date(b.paymentTime) - new Date(a.paymentTime)));
            } catch (e) { console.error(e); }
            finally { setLoading(false); }
        };
        load();
    }, [appliedDateRange]);

    // Format dữ liệu biểu đồ
    const chartData = useMemo(() => {
        const orderCountMap = {};
        orders.forEach(o => {
            const d = new Date(o.paymentTime || o.createdAt);
            const key = fmtDate(d);
            orderCountMap[key] = (orderCountMap[key] || 0) + 1;
        });

        return revenueData.map(item => {
            // Nếu bucket là string "YYYY-MM-DD"
            const dateKey = fmtDate(new Date(item.bucket + 'T00:00:00'));
            return {
                date: dateKey,
                fullDate: fmtDateTime(new Date(item.bucket)),
                revenue: item.revenue,
                orderCount: orderCountMap[dateKey] || 0
            };
        });
    }, [revenueData, orders]);

    // Tính toán KPI
    const kpi = useMemo(() => {
        const totalRev = revenueData.reduce((s, i) => s + i.revenue, 0);
        const totalOrd = orders.length;
        const avg = totalOrd ? Math.round(totalRev / totalOrd) : 0;
        return { totalRev, totalOrd, avg };
    }, [revenueData, orders]);

    const totalPages = Math.ceil(orders.length / ITEMS_PER_PAGE);
    const paginatedOrders = useMemo(() => {
        const start = currentPage * ITEMS_PER_PAGE;
        return orders.slice(start, start + ITEMS_PER_PAGE);
    }, [currentPage, orders]);

    useEffect(() => {
        if (totalPages > 0 && currentPage >= totalPages) {
            setCurrentPage(totalPages - 1);
        }
    }, [currentPage, totalPages]);

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <StatsToolbar dateRange={dateRange} setDateRange={setDateRange} onApply={handleApplyFilters} title="Thời gian" />

            {loading ? <div className="p-20 text-center"><Loader2 className="animate-spin inline text-orange-500" size={32} /></div> : (
                <div className="space-y-6">

                    {/* 1. KPI Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <KpiItem title="Tổng doanh thu" value={fmtVND(kpi.totalRev)} color="text-orange-600" />
                        <KpiItem title="Tổng đơn hàng" value={kpi.totalOrd} color="text-blue-600" />
                        <KpiItem title="Giá trị TB/Đơn" value={fmtVND(kpi.avg)} color="text-teal-600" />
                    </div>

                    {/* 2. Biểu đồ */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Biểu đồ miền (Doanh thu) */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border">
                            <h3 className="font-bold text-gray-800 mb-6">Xu hướng doanh thu</h3>
                            <div className="h-[300px]">
                                <ResponsiveContainer width="100%" height="100%">
                                    <AreaChart data={chartData}>
                                        <defs>
                                            <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#f97316" stopOpacity={0.2} />
                                                <stop offset="95%" stopColor="#f97316" stopOpacity={0} />
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                                        <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} dy={10} />
                                        <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} tickFormatter={(v) => `${v / 1000}k`} />
                                        <RechartsTooltip
                                            formatter={(v) => [fmtVND(v), "Doanh thu"]}
                                            labelFormatter={(l, p) => p[0]?.payload.fullDate}
                                            contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                        />
                                        <Area type="monotone" dataKey="revenue" stroke="#f97316" fill="url(#colorRev)" strokeWidth={3} activeDot={{ r: 6 }} />
                                    </AreaChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        {/* Biểu đồ cột (Số lượng đơn) */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border">
                            <h3 className="font-bold text-gray-800 mb-6">Số lượng đơn hàng</h3>
                            <div className="h-[300px]">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={chartData}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                                        <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} dy={10} />
                                        <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9ca3af', fontSize: 12 }} allowDecimals={false} />
                                        <Tooltip
                                            cursor={{ fill: '#f3f4f6' }}
                                            formatter={(v) => [v, "Đơn hàng"]}
                                            labelFormatter={(l, p) => p[0]?.payload.fullDate}
                                            contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                        />
                                        <Bar dataKey="orderCount" fill="#3b82f6" radius={[4, 4, 0, 0]} barSize={40} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>

                    {/* === 3. BẢNG CHI TIẾT ĐƠN HÀNG (MỚI THÊM) === */}
                    <div className="bg-white rounded-3xl shadow-sm border overflow-hidden">
                        <div className="p-6 border-b border-gray-100 flex items-center justify-between">
                            <h3 className="font-bold text-gray-800 flex items-center gap-2">
                                <Receipt size={20} className="text-gray-500" />
                                Chi tiết giao dịch
                            </h3>
                            <span className="text-sm text-gray-500">{orders.length} đơn hàng</span>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead className="bg-gray-50 text-gray-500 text-xs uppercase font-semibold">
                                    <tr>
                                        <th className="p-4">Mã đơn</th>
                                        <th className="p-4">Thời gian</th>
                                        <th className="p-4">Nhân viên</th>
                                        <th className="p-4 text-center">Trạng thái</th>
                                        <th className="p-4 text-right">Tổng tiền</th>
                                    </tr>
                                </thead>
                                <tbody className="text-sm">
                                    {paginatedOrders.map((order) => (
                                        <tr key={order.id} className="border-b last:border-0 hover:bg-slate-50 transition-colors">
                                            <td className="p-4 font-medium text-blue-600">#{order.id}</td>
                                            <td className="p-4 text-gray-600">
                                                {fmtDateTime(order.paymentTime || order.createdAt)}
                                            </td>
                                            <td className="p-4 text-gray-800 font-medium">
                                                {order.empName || '—'}
                                            </td>
                                            <td className="p-4 text-center">
                                                <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold">
                                                    Đã thanh toán
                                                </span>
                                            </td>
                                            <td className="p-4 text-right font-bold text-gray-800">
                                                {fmtVND(order.totalAmount)}
                                            </td>
                                        </tr>
                                    ))}
                                    {orders.length === 0 && (
                                        <tr key="empty-revenue-row">
                                            <td colSpan="5" className="p-8 text-center text-gray-400">
                                                Không có dữ liệu giao dịch trong khoảng thời gian này.
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                            {totalPages > 1 && (
                                <div className="flex justify-center items-center gap-3 p-4 border-t border-gray-100">
                                    <button
                                        type="button"
                                        disabled={currentPage === 0}
                                        onClick={() => setCurrentPage(prev => prev - 1)}
                                        className="px-4 py-2 rounded-xl border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors shadow-sm font-medium"
                                    >
                                        Trước
                                    </button>
                                    <span className="text-gray-500 font-medium px-4">
                                        Trang {currentPage + 1} / {totalPages}
                                        <span className="text-gray-400 ml-2 text-sm">({orders.length} đơn)</span>
                                    </span>
                                    <button
                                        type="button"
                                        disabled={currentPage >= totalPages - 1}
                                        onClick={() => setCurrentPage(prev => prev + 1)}
                                        className="px-4 py-2 rounded-xl border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors shadow-sm font-medium"
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

// Component con hiển thị thẻ KPI nhỏ (Tốt nhất nên tách ra file riêng như đã bàn, nhưng để tạm ở đây cho chạy được ngay)
const KpiItem = ({ title, value, color }) => (
    <div className="bg-white p-5 rounded-2xl shadow-sm border flex justify-between items-center hover:shadow-md transition-shadow">
        <div>
            <p className="text-gray-500 text-sm font-medium">{title}</p>
            <h3 className={`text-2xl font-black mt-1 ${color}`}>{value}</h3>
        </div>
    </div>
);

export default RevenueStats;
