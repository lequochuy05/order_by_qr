import React, { useState } from 'react';
import { X, CheckCircle, Trash2, Edit3, Save, UtensilsCrossed } from 'lucide-react'; // 1. Đã thêm UtensilsCrossed
import { orderService } from '../../../services/admin/orderService';
import { useAuth } from '../../../context/AuthContext'; // 2. Import Auth

const OrderDetailModal = ({ isOpen, onClose, table, order, onOrderUpdate }) => {
    const [items, setItems] = useState(order?.orderItems || []);
    const [editingId, setEditingId] = useState(null);
    const [editVal, setEditVal] = useState({ quantity: 1, notes: '' });

    // 3. Lấy role từ Context
    const { user } = useAuth();
    const isManager = user?.role === 'MANAGER';

    if (!isOpen || !table) return null;

    const handlePrepared = async (itemId) => {
        try {
            await orderService.markItemPrepared(itemId);
            setItems(prev => prev.map(i => i.id === itemId ? { ...i, prepared: true } : i));
            onOrderUpdate(); 
        } catch { alert("Lỗi cập nhật trạng thái"); }
    };

    const handleDelete = async (itemId) => {
        if (!confirm("Hủy món này khỏi đơn?")) return;
        try {
            await orderService.deleteOrderItem(itemId);
            setItems(prev => prev.filter(i => i.id !== itemId));
            onOrderUpdate();
        } catch { alert("Lỗi hủy món"); }
    };

    const startEdit = (item) => {
        setEditingId(item.id);
        setEditVal({ quantity: item.quantity, notes: item.notes || '' });
    };

    const saveEdit = async (itemId) => {
        try {
            await orderService.updateOrderItem(itemId, editVal);
            setItems(prev => prev.map(i => i.id === itemId ? { ...i, ...editVal } : i));
            setEditingId(null);
            onOrderUpdate();
        } catch { alert("Lỗi cập nhật món"); }
    };

    return (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl w-full max-w-2xl h-[80vh] flex flex-col shadow-2xl overflow-hidden">
                <div className="px-6 py-4 border-b flex justify-between items-center bg-gray-50">
                    <div>
                        <h3 className="font-bold text-lg text-gray-800">
                            Bàn {table.tableNumber} - {items.length > 0 ? 'Chi tiết đơn' : 'Trạng thái'}
                        </h3>
                        <span className={`text-xs px-2 py-0.5 rounded border ${items.length > 0 ? 'bg-green-100 text-green-700 border-green-200' : 'bg-gray-100 text-gray-600 border-gray-200'}`}>
                            {items.length > 0 ? `Mã đơn: #${order?.id}` : 'Bàn trống'}
                        </span>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full transition-colors text-gray-500">
                        <X size={24} />
                    </button>
                </div>
                
                <div className="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar bg-slate-50/50">
                    
                    {/* Empty State */}
                    {items.length === 0 && (
                        <div className="flex flex-col items-center justify-center h-full text-gray-400 space-y-4">
                            <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center">
                                <UtensilsCrossed size={40} className="opacity-50" />
                            </div>
                            <div className="text-center">
                                <p className="text-lg font-medium text-gray-600">Bàn này đang trống</p>
                                <p className="text-sm">Chưa có món ăn nào được gọi.</p>
                            </div>
                            <button onClick={onClose} className="mt-4 px-6 py-2 bg-white border border-gray-300 rounded-lg text-gray-600 hover:bg-gray-50 font-medium shadow-sm">
                                Đóng
                            </button>
                        </div>
                    )}    
                    
                    {items.map(item => {
                        const isCombo = !!item.combo;
                        const name = isCombo ? `Combo ${item.combo.name}` : item.menuItem?.name;
                        const isPrepared = item.prepared;

                        return (
                            <div key={item.id} className={`p-4 rounded-xl border flex gap-4 transition-all ${isPrepared ? 'bg-green-50 border-green-200' : 'bg-white border-gray-200 shadow-sm'}`}>
                                <div className="flex-1">
                                    <div className="font-bold text-gray-800 text-lg flex items-center gap-2">
                                        {name} 
                                        <span className="text-orange-600">x{editingId === item.id ? editVal.quantity : item.quantity}</span>
                                        {isPrepared && <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded-full font-bold">Đã phục vụ</span>}
                                    </div>
                                    
                                    {editingId === item.id ? (
                                        <div className="mt-2 flex gap-2">
                                            <input type="number" min="1" className="w-20 p-2 border rounded-lg outline-none focus:ring-2 ring-blue-500" value={editVal.quantity} onChange={e => setEditVal({...editVal, quantity: parseInt(e.target.value)})}/>
                                            <input type="text" className="flex-1 p-2 border rounded-lg outline-none focus:ring-2 ring-blue-500" placeholder="Ghi chú..." value={editVal.notes} onChange={e => setEditVal({...editVal, notes: e.target.value})}/>
                                        </div>
                                    ) : (
                                        item.notes && <p className="text-sm text-gray-500 italic mt-1 bg-gray-50 p-1.5 rounded-lg inline-block">Note: {item.notes}</p>
                                    )}
                                </div>

                                <div className="flex flex-col gap-2 justify-center">
                                    {isPrepared ? (
                                        <CheckCircle className="text-green-500" size={28}/>
                                    ) : (
                                        <>
                                            {editingId === item.id ? (
                                                <button onClick={() => saveEdit(item.id)} className="p-2 bg-blue-100 text-blue-600 rounded-lg hover:bg-blue-200"><Save size={20}/></button>
                                            ) : (
                                                <div className="flex gap-2">
                                                    {/* Nút Báo Xong: Ai cũng thấy */}
                                                    <button onClick={() => handlePrepared(item.id)} className="px-3 py-2 bg-green-100 text-green-700 rounded-lg text-sm font-bold hover:bg-green-200 transition-colors">Xong</button>
                                                    
                                                    {/* Nút Sửa / Xóa: Chỉ Manager thấy */}
                                                    {isManager && (
                                                        <>
                                                            <button onClick={() => startEdit(item)} className="p-2 bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200 transition-colors"><Edit3 size={18}/></button>
                                                            <button onClick={() => handleDelete(item.id)} className="p-2 bg-red-50 text-red-500 rounded-lg hover:bg-red-100 transition-colors"><Trash2 size={18}/></button>
                                                        </>
                                                    )}
                                                </div>
                                            )}
                                        </>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default OrderDetailModal;