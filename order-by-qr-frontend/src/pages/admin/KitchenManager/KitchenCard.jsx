import React from 'react';
import { fmtTime } from '../../../utils/formatters';

const KitchenCard = ({ item, status, onUpdateStatus, isProcessing }) => {
    return (
        <div key={item.id} className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 hover:shadow-md transition-shadow">
            <div className="flex justify-between items-start mb-2">
                <span className="bg-orange-100 text-orange-700 px-2 py-1 rounded-md text-sm font-bold">
                    Bàn: {item.tableName}
                </span>
                <span className="text-xs text-slate-400">
                    {fmtTime(item.createdAt)}
                </span>
            </div>

            <h3 className="font-semibold text-slate-800 text-lg">
                {item.menuItem?.name || item.combo?.name || 'NaN'}
                {item.quantity > 1 && <span className="text-orange-600 ml-2">x{item.quantity}</span>}
            </h3>

            {item.notes && (
                <div className="mt-2 p-2 bg-yellow-50 border border-yellow-100 rounded text-sm text-yellow-800 italic">
                    Ghi chú: {item.notes}
                </div>
            )}

            <div className="mt-4 flex gap-2">
                {status === 'PENDING' && (
                    <button
                        onClick={() => onUpdateStatus(item.id, 'COOKING')}
                        disabled={isProcessing}
                        className={`flex-1 text-white py-2 rounded-lg text-sm font-medium transition-colors ${isProcessing ? 'bg-indigo-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700'}`}
                    >
                        {isProcessing ? 'Đang xử lý...' : 'Bắt đầu nấu'}
                    </button>
                )}
                {status === 'COOKING' && (
                    <button
                        onClick={() => onUpdateStatus(item.id, 'FINISHED')}
                        disabled={isProcessing}
                        className={`flex-1 text-white py-2 rounded-lg text-sm font-medium transition-colors ${isProcessing ? 'bg-green-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700'}`}
                    >
                        {isProcessing ? 'Đang xử lý...' : 'Hoàn thành'}
                    </button>
                )}
                {status === 'FINISHED' && (
                    <button
                        onClick={() => onUpdateStatus(item.id, 'COOKING')}
                        disabled={isProcessing}
                        className={`text-xs flex items-center gap-1 mx-auto mt-1 ${isProcessing ? 'text-slate-300 cursor-not-allowed' : 'text-slate-400 hover:text-slate-600'}`}
                    >
                        {isProcessing ? 'Đang hoàn tác...' : '↩ Hoàn tác'}
                    </button>
                )}
            </div>
        </div>
    );
};

export default KitchenCard;
