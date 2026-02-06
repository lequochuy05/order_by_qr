import React from 'react';
import { Users, FileText, PlusCircle, CreditCard, Edit, Trash2, Lock } from 'lucide-react';

const TableCard = ({ table, order, onDetail, onAddItems, onPay, onEdit, onDelete, userRole }) => {
    const hasOrder = order && order.totalAmount > 0;
    const isManager = userRole === 'MANAGER'; // Kiểm tra quyền

    const statusColor = {
        'Trống': 'bg-gray-100 border-gray-200',
        'Đang phục vụ': 'bg-green-50 border-green-200',
        'Chờ thanh toán': 'bg-yellow-50 border-yellow-200'
    }[table.status] || 'bg-white';

    return (
        <div className={`relative p-4 rounded-2xl border-2 transition-all hover:shadow-lg ${statusColor} group`}>
            {/* Header và Nội dung chính giữ nguyên */}
            <div className="flex justify-between items-start mb-2">
                <h3 className="text-xl font-bold text-gray-800">Bàn {table.tableNumber}</h3>
                <span className="text-xs font-bold uppercase px-2 py-1 rounded bg-white/50 border">
                    {table.status}
                </span>
            </div>

            <div className="flex items-center gap-2 text-gray-500 text-sm mb-4">
                <Users size={16} /> <span>{table.capacity} khách</span>
            </div>

            <div className="mb-4">
                <p className="text-sm text-gray-500">Tạm tính:</p>
                <p className="text-2xl font-black text-orange-600">
                    {hasOrder ? order.totalAmount.toLocaleString() : 0} ₫
                </p>
            </div>

            <div className="grid grid-cols-2 gap-2">
                <button onClick={onDetail} className="btn-secondary flex items-center justify-center gap-1 py-2 rounded-lg bg-white border hover:bg-gray-50">
                    <FileText size={16}/> Chi tiết
                </button>
                <button onClick={onAddItems} className="btn-primary flex items-center justify-center gap-1 py-2 rounded-lg bg-orange-100 text-orange-600 hover:bg-orange-200">
                    <PlusCircle size={16}/> Thêm món
                </button>
                
                {hasOrder && (
                    <button onClick={onPay} className="col-span-2 btn-pay flex items-center justify-center gap-1 py-2 rounded-lg bg-green-500 text-white hover:bg-green-600 shadow-md">
                        <CreditCard size={16}/> Thanh toán
                    </button>
                )}
            </div>
            
            {/* */}
            <div className="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                 <button onClick={onEdit} className="p-1.5 bg-white rounded-lg shadow-sm text-blue-500 hover:bg-blue-50 border border-gray-100" title="Sửa bàn">
                    {isManager ? <Edit size={14}/> : <Lock size={14} className="text-gray-400"/>}
                 </button>
                 <button onClick={onDelete} className="p-1.5 bg-white rounded-lg shadow-sm text-red-500 hover:bg-red-50 border border-gray-100" title="Xóa bàn">
                    {isManager ? <Trash2 size={14}/> : <Lock size={14} className="text-gray-400"/>}
                 </button>
            </div>
        </div>
    );
};

export default TableCard;