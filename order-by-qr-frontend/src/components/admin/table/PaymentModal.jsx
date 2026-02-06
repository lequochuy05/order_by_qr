import React, { useState, useEffect } from 'react';
import { X, CreditCard, Tag } from 'lucide-react';
import { orderService } from '../../../services/admin/orderService';
import { printInvoice } from '../../../utils/invoiceGenerator';

const PaymentModal = ({ isOpen, onClose, table, order, onPaymentSuccess }) => {
    const [voucherCode, setVoucherCode] = useState('');
    const [previewData, setPreviewData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (isOpen && table && order) {
            setVoucherCode('');
            setError('');
            loadPreview('');
        }
    }, [isOpen, table, order]);

    const loadPreview = async (code) => {
        setLoading(true);
        try {
            const items = order.orderItems.filter(i => i.menuItem).map(i => ({ menuItemId: i.menuItem.id, quantity: i.quantity, notes: i.notes }));
            const combos = order.orderItems.filter(i => i.combo).map(i => ({ comboId: i.combo.id, quantity: i.quantity, notes: i.notes }));

            const res = await orderService.previewOrder({
                tableId: table.id,
                items, combos,
                voucherCode: code || null // Nếu code rỗng thì gửi null
            });
            setPreviewData(res);
            
            // Fix lỗi UI: Nếu có code mà invalid -> báo lỗi. 
            if (code && !res.voucherValid) setError(res.voucherMessage || "Voucher không hợp lệ");
            else setError('');

        } catch (e) {
            setError("Lỗi tính toán hóa đơn");
        } finally {
            setLoading(false);
        }
    };

    // FIX LỖI VOUCHER 1: Xử lý khi thay đổi input
    const handleInputChange = (e) => {
        const val = e.target.value;
        setVoucherCode(val);
        // Nếu xóa trắng input -> Tự động tính lại giá gốc (bỏ voucher)
        if (val.trim() === '') {
            loadPreview('');
        }
    };

    const handleApplyVoucher = () => {
        if (!voucherCode.trim()) return;
        loadPreview(voucherCode);
    };

    const handleConfirmPay = async () => {
        if (!confirm(`Xác nhận thanh toán cho bàn ${table.tableNumber}?`)) return;
        try {
            const userId = localStorage.getItem('userId');
            // FIX LỖI VOUCHER 2: Đảm bảo gửi đúng mã đang hiện trong ô input
            const finalVoucher = voucherCode.trim() === '' ? null : voucherCode;
            
            await orderService.payOrder(order.id, userId, finalVoucher);
            
            printInvoice({
                order: { ...order, ...previewData, totalAmount: previewData.finalTotal },
                table,
                paidBy: localStorage.getItem('fullname') || 'Admin',
                paidAt: new Date()
            });

            onPaymentSuccess();
            onClose();
        } catch (e) {
            alert("Thanh toán thất bại: " + (e.response?.data?.message || e.message));
        }
    };

    if (!isOpen || !table) return null;

    const subTotal = (previewData?.subtotalItems || 0) + (previewData?.subtotalCombos || 0);
    const discount = previewData?.discountVoucher || 0;
    const finalTotal = previewData?.finalTotal || 0;

    return (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 animate-in fade-in zoom-in duration-200">
            <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden">
                <div className="px-6 py-4 border-b bg-gray-50 flex justify-between items-center">
                    <h3 className="font-bold text-lg">Thanh toán - Bàn {table.tableNumber}</h3>
                    <button onClick={onClose}><X size={20} className="text-gray-500 hover:text-red-500"/></button>
                </div>

                <div className="p-6 space-y-6">
                    {/* Voucher Input */}
                    <div className="flex gap-2">
                        <div className="relative flex-1">
                            <Tag className="absolute left-3 top-3 text-gray-400" size={18}/>
                            <input type="text" className="w-full pl-10 pr-4 py-2.5 border rounded-xl uppercase focus:ring-2 focus:ring-orange-500 outline-none"
                                placeholder="Mã giảm giá / Voucher"
                                value={voucherCode} 
                                onChange={handleInputChange} // Dùng hàm mới
                            />
                        </div>
                        <button onClick={handleApplyVoucher} disabled={loading} className="px-4 bg-blue-500 text-white rounded-xl font-bold hover:bg-blue-600 disabled:opacity-50">
                            Áp dụng
                        </button>
                    </div>
                    {error && <p className="text-red-500 text-sm">{error}</p>}
                    {/* Chỉ hiện thông báo xanh nếu voucher hợp lệ VÀ input không rỗng */}
                    {previewData?.voucherValid && voucherCode && <p className="text-green-600 text-sm font-medium">✅ Áp dụng voucher thành công: -{discount.toLocaleString()}đ</p>}

                    {/* Summary */}
                    <div className="bg-gray-50 p-4 rounded-xl space-y-2 text-sm">
                        <div className="flex justify-between">
                            <span className="text-gray-500">Tạm tính:</span>
                            <span className="font-medium">{subTotal.toLocaleString()}đ</span>
                        </div>
                        {discount > 0 && (
                            <div className="flex justify-between text-green-600">
                                <span>Giảm giá:</span>
                                <span>-{discount.toLocaleString()}đ</span>
                            </div>
                        )}
                        <div className="border-t pt-2 mt-2 flex justify-between text-lg font-bold text-gray-800">
                            <span>Tổng thanh toán:</span>
                            <span className="text-orange-600">{finalTotal.toLocaleString()}đ</span>
                        </div>
                    </div>
                </div>

                <div className="px-6 py-4 border-t flex gap-3">
                    <button onClick={onClose} className="flex-1 py-3 text-gray-600 font-bold hover:bg-gray-100 rounded-xl">Hủy</button>
                    <button onClick={handleConfirmPay} disabled={loading} className="flex-[2] py-3 bg-green-500 text-white font-bold rounded-xl hover:bg-green-600 shadow-lg shadow-green-200 flex items-center justify-center gap-2">
                        <CreditCard size={20}/> Xác nhận & In hóa đơn
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PaymentModal;