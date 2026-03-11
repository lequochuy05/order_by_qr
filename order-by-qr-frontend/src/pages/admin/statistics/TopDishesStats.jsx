import React, { useState, useEffect, useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Loader2 } from 'lucide-react';
import { statisticsService } from '../../../services/admin/statisticsService';
import StatsToolbar from '../../../components/admin/stats/StatsToolbar';
import ManagementHeader from '../../../components/admin/common/ManagementHeader';

const TopDishesStats = () => {
    const [dateRange, setDateRange] = useState({ 
        from: new Date(new Date().setDate(new Date().getDate() - 6)), 
        to: new Date() 
    });
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                // Lấy danh sách đơn hàng để phân tích món
                const data = await statisticsService.getOrders(dateRange.from, dateRange.to);
                setOrders(data);
            } catch(e) { console.error(e); } 
            finally { setLoading(false); }
        };
        load();
    }, [dateRange]);

    // === LOGIC QUAN TRỌNG: TỔNG HỢP MÓN ĂN ===
    const topDishes = useMemo(() => {
        const map = {};
        orders.forEach(order => {
            // Chỉ tính các đơn đã thanh toán hoặc đang phục vụ (tuỳ logic quán)
            if (order.status === 'CANCELLED') return;

            if(order.orderItems) {
                order.orderItems.forEach(item => {
                    const name = item.menuItem ? item.menuItem.name : (item.combo ? `Combo ${item.combo.name}` : 'Món không tên');
                    const qty = item.quantity || 0;
                    const price = item.unitPrice || 0;
                    
                    if(!map[name]) map[name] = { name, quantity: 0, revenue: 0 };
                    map[name].quantity += qty;
                    map[name].revenue += (qty * price);
                });
            }
        });

        // Chuyển map thành array và sort giảm dần theo số lượng
        return Object.values(map)
            .sort((a, b) => b.quantity - a.quantity)
            .slice(0, 10); // Lấy Top 10
    }, [orders]);

    const fmtVND = (n) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n);
    const COLORS = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#6366f1'];

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader title="Top món ăn bán chạy" />
            <StatsToolbar dateRange={dateRange} setDateRange={setDateRange} title="Thời gian" />

            {loading ? <div className="p-20 text-center"><Loader2 className="animate-spin inline text-orange-500" size={32}/></div> : (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
                    
                    {/* Biểu đồ thanh ngang (Horizontal Bar Chart) */}
                    <div className="bg-white p-6 rounded-3xl shadow-sm border">
                        <h3 className="font-bold text-gray-800 mb-4">Top 5 Món theo số lượng</h3>
                        <div className="h-[400px]">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart layout="vertical" data={topDishes.slice(0,5)} margin={{left: 10, right: 30}}>
                                    <XAxis type="number" hide />
                                    <YAxis dataKey="name" type="category" width={140} tick={{fontSize: 12, fill: '#4b5563'}} />
                                    <Tooltip 
                                        cursor={{fill: 'transparent'}}
                                        contentStyle={{borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)'}}
                                        formatter={(value) => [value, 'Đã bán']} 
                                    />
                                    <Bar dataKey="quantity" radius={[0, 4, 4, 0]} barSize={24}>
                                        {topDishes.slice(0,5).map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Bảng chi tiết */}
                    <div className="bg-white p-6 rounded-3xl shadow-sm border overflow-hidden">
                        <h3 className="font-bold text-gray-800 mb-4">Chi tiết Top 10</h3>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
                                    <tr>
                                        <th className="p-3 rounded-l-lg">Tên món</th>
                                        <th className="p-3 text-right">Số lượng</th>
                                        <th className="p-3 text-right rounded-r-lg">Doanh thu</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {topDishes.map((dish, idx) => (
                                        <tr key={idx} className="border-b last:border-0 hover:bg-slate-50 transition-colors">
                                            <td className="p-3 font-medium flex items-center gap-3">
                                                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white shadow-sm ${idx < 3 ? 'bg-orange-500' : 'bg-gray-400'}`}>
                                                    {idx + 1}
                                                </span>
                                                <span className="text-gray-700">{dish.name}</span>
                                            </td>
                                            <td className="p-3 text-right font-bold text-gray-800">{dish.quantity}</td>
                                            <td className="p-3 text-right text-gray-600">{fmtVND(dish.revenue)}</td>
                                        </tr>
                                    ))}
                                    {topDishes.length === 0 && (
                                        <tr><td colSpan="3" className="p-8 text-center text-gray-400">Không có dữ liệu đơn hàng nào</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default TopDishesStats;