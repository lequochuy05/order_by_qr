import React, { useState, useEffect, useMemo } from 'react';
import { 
    BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid, 
    ResponsiveContainer, Tooltip as RechartsTooltip, Tooltip 
} from 'recharts';
import { Loader2, Receipt } from 'lucide-react'; // Thêm icon Receipt
import { statisticsService } from '../../../services/admin/statisticsService';
import StatsToolbar from '../../../components/admin/stats/StatsToolbar';
import { fmtVND, fmtDate, fmtDateTime } from '../../../utils/formatters';

const RevenueStats = () => {
    // Mặc định 7 ngày  
    const [dateRange, setDateRange] = useState({ 
        from: new Date(new Date().setDate(new Date().getDate() - 6)), 
        to: new Date() 
    });
    const [revenueData, setRevenueData] = useState([]);
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);

    // Gọi API khi thay đổi ngày
    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [rev, ods] = await Promise.all([
                    statisticsService.getRevenue(dateRange.from, dateRange.to),
                    statisticsService.getOrders(dateRange.from, dateRange.to)
                ]);
                setRevenueData(rev);
                // Sắp xếp đơn hàng mới nhất lên đầu để hiển thị trong bảng
                setOrders(ods.sort((a, b) => new Date(b.paymentTime) - new Date(a.paymentTime)));
            } catch(e) { console.error(e); } 
            finally { setLoading(false); }
        };
        load();
    }, [dateRange]);

    // Format dữ liệu biểu đồ
    const chartData = useMemo(() => {
        const orderCountMap = {};
        orders.forEach(o => {
            const d = new Date(o.paymentTime || o.createdAt); 
            const key = fmtDate(d); 
            orderCountMap[key] = (orderCountMap[key] || 0) + 1;
        });

        return revenueData.map(item => {
            const dateKey = fmtDate(new Date(item.bucket));
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

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <StatsToolbar dateRange={dateRange} setDateRange={setDateRange} title="Thời gian" />

            {loading ? <div className="p-20 text-center"><Loader2 className="animate-spin inline text-orange-500" size={32}/></div> : (
                <div className="space-y-6">
                    
                    {/* 1. KPI Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <KpiItem title="Tổng doanh thu" value={fmtVND(kpi.totalRev)} color="text-orange-600" />
                        <KpiItem title="Tổng đơn hàng" value={kpi.totalOrd}  color="text-blue-600"/>
                        <KpiItem title="Giá trị TB/Đơn" value={fmtVND(kpi.avg)} color="text-purple-600"/>
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
                                                <stop offset="5%" stopColor="#f97316" stopOpacity={0.2}/>
                                                <stop offset="95%" stopColor="#f97316" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                                        <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} dy={10} />
                                        <YAxis axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} tickFormatter={(v) => `${v/1000}k`} />
                                        <RechartsTooltip 
                                            formatter={(v) => [fmtVND(v), "Doanh thu"]} 
                                            labelFormatter={(l, p) => p[0]?.payload.fullDate}
                                            contentStyle={{borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)'}}
                                        />
                                        <Area type="monotone" dataKey="revenue" stroke="#f97316" fill="url(#colorRev)" strokeWidth={3} activeDot={{r: 6}} />
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
                                        <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} dy={10} />
                                        <YAxis axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} allowDecimals={false} />
                                        <Tooltip 
                                            cursor={{fill: '#f3f4f6'}}
                                            formatter={(v) => [v, "Đơn hàng"]}
                                            labelFormatter={(l, p) => p[0]?.payload.fullDate}
                                            contentStyle={{borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)'}}
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
                                <Receipt size={20} className="text-gray-500"/>
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
                                    {orders.slice(0, 10).map((order) => ( // Chỉ hiện 10 đơn mới nhất để tránh quá dài
                                        <tr key={order.id} className="border-b last:border-0 hover:bg-slate-50 transition-colors">
                                            <td className="p-4 font-medium text-blue-600">#{order.id}</td>
                                            <td className="p-4 text-gray-600">
                                                {fmtDateTime(order.paymentTime || order.createdAt)}
                                            </td>
                                            <td className="p-4 text-gray-800 font-medium">
                                                {order.employeeName || '—'}
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
                                        <tr>
                                            <td colSpan="5" className="p-8 text-center text-gray-400">
                                                Không có dữ liệu giao dịch trong khoảng thời gian này.
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                            {orders.length > 10 && (
                                <div className="p-3 text-center border-t border-gray-100">
                                    <button className="text-sm text-blue-600 font-medium hover:underline">
                                        Xem tất cả đơn hàng
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