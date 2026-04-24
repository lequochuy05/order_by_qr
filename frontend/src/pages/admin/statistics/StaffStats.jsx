import React, { useState, useEffect, useMemo } from 'react';
import {
    PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend
} from 'recharts';
import { Loader2, Award, Users } from 'lucide-react';
import { statisticsService } from '../../../services/admin/statisticsService';
import StatsToolbar from '../../../components/admin/common/StatsToolbar';

import { fmtVND } from '../../../utils/formatters';

const StaffStats = () => {
    // State thời gian
    const [dateRange, setDateRange] = useState({
        from: new Date(new Date().setDate(new Date().getDate() - 6)),
        to: new Date()
    });
    const [employees, setEmployees] = useState([]);
    const [loading, setLoading] = useState(false);

    // Load data
    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const data = await statisticsService.getEmployees(dateRange.from, dateRange.to);
                // Sắp xếp giảm dần theo doanh thu
                setEmployees(data.sort((a, b) => (b.revenue || 0) - (a.revenue || 0)));
            } catch (e) { console.error(e); }
            finally { setLoading(false); }
        };
        load();
    }, [dateRange]);

    // === 1. TÍNH TOÁN DỮ LIỆU BIỂU ĐỒ ===
    const pieData = useMemo(() => {
        if (!employees.length) return [];

        // Lấy Top 5 nhân viên
        const top5 = employees.slice(0, 5);

        // Tính tổng phần còn lại
        const othersRevenue = employees.slice(5).reduce((acc, curr) => acc + (curr.revenue || 0), 0);

        const data = top5.map(e => ({
            name: e.fullName,
            value: e.revenue || 0
        }));

        // Nếu có nhóm "Khác" thì push vào
        if (othersRevenue > 0) {
            data.push({ name: 'Khác (Còn lại)', value: othersRevenue });
        }

        return data;
    }, [employees]);

    // Màu sắc cho biểu đồ (Top 1 -> Top 5 -> Khác)
    const COLORS = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#9ca3af'];

    // Tìm max revenue cho progress bar
    const maxRev = Math.max(...employees.map(e => e.revenue || 0), 1);

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <StatsToolbar dateRange={dateRange} setDateRange={setDateRange} title="Thời gian" />

            {loading ? (
                <div className="p-20 text-center"><Loader2 className="animate-spin inline text-orange-500" size={32} /></div>
            ) : (
                <div className="flex flex-col lg:flex-row gap-6 mt-6">

                    {/* === PHẦN TRÁI: BIỂU ĐỒ TRÒN === */}
                    <div className="lg:w-1/3 bg-white p-6 rounded-3xl shadow-sm border h-fit sticky top-6">

                        <div className="h-[300px] w-full relative">
                            {pieData.length > 0 ? (
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={pieData}
                                            innerRadius={60} // Tạo hiệu ứng Donut
                                            outerRadius={80}
                                            paddingAngle={5}
                                            dataKey="value"
                                        >
                                            {pieData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                        <RechartsTooltip
                                            formatter={(value) => fmtVND(value)}
                                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                        />
                                        <Legend verticalAlign="bottom" height={36} iconType="circle" />
                                    </PieChart>
                                </ResponsiveContainer>
                            ) : (
                                <div className="flex flex-col items-center justify-center h-full text-gray-400">
                                    <Users size={48} className="opacity-20 mb-2" />
                                    <p>Chưa có dữ liệu</p>
                                </div>
                            )}

                            {/* Tổng doanh thu ở giữa biểu đồ */}
                            {pieData.length > 0 && (
                                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                                    <span className="text-xs text-gray-400 font-medium">Tổng</span>
                                    <span className="text-sm font-bold text-gray-800">
                                        {fmtVND(pieData.reduce((a, b) => a + b.value, 0))}
                                    </span>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* === PHẦN PHẢI: DANH SÁCH CHI TIẾT === */}
                    <div className="lg:w-2/3">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {employees.map((emp, idx) => (
                                <div key={idx} className="bg-white border border-gray-100 rounded-2xl p-5 hover:shadow-lg transition-all duration-300 relative overflow-hidden group">
                                    {/* Huy hiệu Top 1-3 */}
                                    {idx < 3 && (
                                        <div className={`absolute top-0 right-0 text-xs font-bold px-3 py-1 rounded-bl-xl flex items-center gap-1
                                            ${idx === 0 ? 'bg-yellow-100 text-yellow-700' :
                                                idx === 1 ? 'bg-gray-200 text-gray-700' :
                                                    'bg-orange-100 text-orange-800'}`}>
                                            <Award size={14} /> Top {idx + 1}
                                        </div>
                                    )}

                                    <div className="flex items-center gap-4 mb-4">
                                        {/* Avatar giả lập màu sắc theo thứ hạng */}
                                        <div className={`w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold shadow-sm text-white
                                            ${idx === 0 ? 'bg-yellow-500' :
                                                idx === 1 ? 'bg-gray-400' :
                                                    idx === 2 ? 'bg-orange-400' : 'bg-blue-100 text-blue-600'}`}>
                                            {emp.fullName ? emp.fullName.charAt(0).toUpperCase() : '?'}
                                        </div>
                                        <div>
                                            <h4 className="font-bold text-gray-800 text-lg line-clamp-1">{emp.fullName}</h4>
                                            <p className="text-sm text-gray-500">{emp.orders} đơn hàng</p>
                                        </div>
                                    </div>

                                    <div className="space-y-3">
                                        <div className="flex justify-between text-sm items-end">
                                            <span className="text-gray-500">Doanh thu</span>
                                            <span className="font-bold text-green-600 text-lg">{fmtVND(emp.revenue)}</span>
                                        </div>

                                        {/* Thanh Progress bar đồng bộ màu */}
                                        <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                                            <div
                                                className="h-2.5 rounded-full transition-all duration-1000 ease-out"
                                                style={{
                                                    width: `${(emp.revenue / maxRev) * 100}%`,
                                                    backgroundColor: idx < 5 ? COLORS[idx] : '#9ca3af' // Đồng bộ màu với biểu đồ
                                                }}
                                            ></div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {employees.length === 0 && (
                            <div className="bg-white p-10 rounded-3xl border border-dashed text-center text-gray-400">
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