import React from 'react';
import { Calendar, Download } from 'lucide-react';

const StatsToolbar = ({ dateRange, setDateRange, onExport, title }) => {
    // Helper đổi ngày
    const handleDateChange = (type, value) => {
        const newDate = new Date(value);
        if (!isNaN(newDate.getTime())) {
            setDateRange(prev => ({ ...prev, [type]: newDate }));
        }
    };

    const setPreset = (days) => {
        const to = new Date(); to.setHours(0,0,0,0);
        const from = new Date(to); from.setDate(to.getDate() - (days - 1));
        setDateRange({ from, to });
    };

    return (
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
            <div className="flex items-center gap-3 flex-wrap">
                <span className="font-bold text-gray-700 mr-2">{title}</span>
                <div className="flex items-center gap-2 bg-gray-50 px-3 py-2 rounded-lg border">
                    <Calendar size={18} className="text-gray-500" />
                    <input type="date" className="bg-transparent border-none outline-none text-sm font-medium text-gray-700"
                        value={dateRange.from.toISOString().split('T')[0]}
                        onChange={(e) => handleDateChange('from', e.target.value)}
                    />
                    <span className="text-gray-400">-</span>
                    <input type="date" className="bg-transparent border-none outline-none text-sm font-medium text-gray-700"
                        value={dateRange.to.toISOString().split('T')[0]}
                        onChange={(e) => handleDateChange('to', e.target.value)}
                    />
                </div>
                <div className="flex gap-2">
                    <button onClick={() => setPreset(7)} className="px-3 py-2 text-xs font-bold bg-white border text-gray-600 hover:bg-gray-50 rounded-lg">7 ngày</button>
                    <button onClick={() => setPreset(30)} className="px-3 py-2 text-xs font-bold bg-white border text-gray-600 hover:bg-gray-50 rounded-lg">30 ngày</button>
                </div>
            </div>
            {onExport && (
                <button onClick={onExport} className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white font-bold rounded-xl hover:bg-green-700 text-sm">
                    <Download size={16} /> Xuất CSV
                </button>
            )}
        </div>
    );
};

export default StatsToolbar;