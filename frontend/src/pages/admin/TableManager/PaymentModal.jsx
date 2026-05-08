import React, { useState, useEffect, useCallback, useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, CreditCard, Tag, QrCode, Banknote, RefreshCw, XCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { orderService } from '../../../services/admin/orderService';
import { paymentService } from '../../../services/admin/paymentService';
import { printInvoice } from '../../../utils/invoiceGenerator';

const PaymentModal = ({ isOpen, onClose, table, order, onPaymentSuccess }) => {
    const [voucherCode, setVoucherCode] = useState('');
    const [previewData, setPreviewData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // PayOS states
    const [paymentMethod, setPaymentMethod] = useState('CASH'); // 'CASH' or 'PAYOS'
    const [payosLoading, setPayosLoading] = useState(false);
    const [payosData, setPayosData] = useState(null);
    const [payosStatus, setPayosStatus] = useState('idle'); // 'idle', 'waiting', 'success', 'expired', 'error'
    const [timeLeft, setTimeLeft] = useState(0);
    const pollingRef = useRef(null);

    useEffect(() => {
        let timer;
        if (payosStatus === 'waiting' && payosData?.createdAt) {
            const calculateTimeLeft = () => {
                const created = new Date(payosData.createdAt).getTime();
                const now = new Date().getTime();
                const diff = Math.floor((created + 20 * 60 * 1000 - now) / 1000);
                return diff > 0 ? diff : 0;
            };

            const initial = calculateTimeLeft();
            setTimeLeft(initial);
            if (initial <= 0) setPayosStatus('expired');

            timer = setInterval(() => {
                const remaining = calculateTimeLeft();
                setTimeLeft(remaining);
                if (remaining <= 0) {
                    setPayosStatus('expired');
                    clearInterval(timer);
                    if (pollingRef.current) clearInterval(pollingRef.current);
                }
            }, 1000);
        }
        return () => clearInterval(timer);
    }, [payosStatus, payosData]);

    const formatTime = (seconds) => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    };

    const loadPreview = useCallback(async (code) => {
        setLoading(true);
        try {
            const items = order.orderItems.filter(i => i.menuItem).map(i => ({ menuItemId: i.menuItem.id, quantity: i.quantity, notes: i.notes }));
            const combos = order.orderItems.filter(i => i.combo).map(i => ({ comboId: i.combo.id, quantity: i.quantity, notes: i.notes }));

            const res = await orderService.previewOrder({
                tableId: table.id,
                items, combos,
                voucherCode: code || null
            });
            setPreviewData(res);

            if (code && !res.voucherValid) setError(res.voucherMessage || "Voucher không hợp lệ");
            else setError('');

        } catch (_e) {
            setError("Lỗi tính toán hóa đơn");
        } finally {
            setLoading(false);
        }
    }, [order, table?.id]);

    useEffect(() => {
        if (isOpen && table && order) {
            loadPreview('');
            setPaymentMethod('CASH');
            setPayosStatus('idle');
            setPayosData(null);
        }
        return () => {
            if (pollingRef.current) clearInterval(pollingRef.current);
        };
    }, [isOpen, table, order, loadPreview]);

    const handleInputChange = (e) => {
        const val = e.target.value;
        setVoucherCode(val);
        if (val.trim() === '') loadPreview('');
    };

    const handleApplyVoucher = () => {
        if (!voucherCode.trim()) return;
        loadPreview(voucherCode);
    };

    // PayOS Logic
    const handleCreatePayosQR = async () => {
        setPayosLoading(true);
        setError('');
        try {
            const data = await paymentService.createPaymentLink(order.id, previewData.finalTotal);
            setPayosData(data);
            setPayosStatus('waiting');

            // Bắt đầu polling kiểm tra trạng thái (fallback nếu WebSocket chậm)
            startPolling(data.transactionId);
        } catch (e) {
            setError(e.response?.data?.message || "Không thể tạo mã QR thanh toán");
            setPayosStatus('error');
        } finally {
            setPayosLoading(false);
        }
    };

    const startPolling = (transactionId) => {
        if (pollingRef.current) clearInterval(pollingRef.current);
        pollingRef.current = setInterval(async () => {
            try {
                const status = await paymentService.syncPaymentStatus(transactionId);
                if (status.status === 'PAID') {
                    handlePaymentSuccess(status);
                }
            } catch (e) {
                console.error("Polling error:", e);
            }
        }, 3000);
    };

    const handleCancelPayos = async () => {
        if (!payosData) return;
        try {
            await paymentService.cancelPaymentLink(payosData.transactionId, "Người dùng hủy trên UI");
            setPayosStatus('idle');
            setPayosData(null);
            if (pollingRef.current) clearInterval(pollingRef.current);
        } catch (e) {
            alert("Không thể hủy giao dịch PayOS");
        }
    };

    const handlePaymentSuccess = (transactionData) => {
        if (pollingRef.current) clearInterval(pollingRef.current);
        setPayosStatus('success');

        // Tự động đóng và in hóa đơn sau 2s thành công
        setTimeout(() => {
            finishPayment();
        }, 2000);
    };

    const finishPayment = () => {
        printInvoice({
            order: { ...order, ...previewData, totalAmount: previewData.finalTotal },
            table,
            paidBy: localStorage.getItem('fullname') || 'Admin',
            paidAt: new Date(),
            paymentMethod: paymentMethod === 'PAYOS' ? 'Chuyển khoản (PayOS)' : 'Tiền mặt'
        });

        onPaymentSuccess();
        onClose();
    };

    const handleConfirmCashPay = async () => {
        if (!confirm(`Xác nhận thanh toán TIỀN MẶT cho bàn ${table.tableNumber}?`)) return;
        try {
            const userId = localStorage.getItem('userId');
            const finalVoucher = voucherCode.trim() === '' ? null : voucherCode;
            await orderService.payOrder(order.id, userId, finalVoucher);
            finishPayment();
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
            <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
                <div className="px-6 py-4 border-b bg-gray-50 flex justify-between items-center shrink-0">
                    <div>
                        <h3 className="font-bold text-lg">Thanh toán hóa đơn</h3>
                        <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold">Bàn {table.tableNumber} • #{order.id.toString().slice(-6)}</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full transition-colors">
                        <X size={20} className="text-gray-500" />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    {/* Method Selection */}
                    <div className="flex gap-3">
                        <button
                            onClick={() => { setPaymentMethod('CASH'); setPayosStatus('idle'); setPayosData(null); }}
                            disabled={payosStatus === 'waiting'}
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl font-bold transition-all
                                ${paymentMethod === 'CASH'
                                    ? 'bg-green-50 text-green-700 border-2 border-green-500 shadow-sm'
                                    : 'bg-gray-100 text-gray-500 border-2 border-transparent hover:bg-gray-200'}
                                ${payosStatus === 'waiting' ? 'opacity-50 cursor-not-allowed' : ''}
                            `}
                        >
                            <Banknote size={20} /> Tiền mặt
                        </button>
                        <button
                            onClick={() => setPaymentMethod('PAYOS')}
                            disabled={payosStatus === 'waiting' && paymentMethod !== 'PAYOS'}
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl font-bold transition-all
                                ${paymentMethod === 'PAYOS'
                                    ? 'bg-blue-50 text-blue-700 border-2 border-blue-500 shadow-sm'
                                    : 'bg-gray-100 text-gray-500 border-2 border-transparent hover:bg-gray-200'}
                            `}
                        >
                            <QrCode size={20} /> Chuyển khoản
                        </button>
                    </div>

                    {paymentMethod === 'PAYOS' ? (
                        <div className="space-y-4 animate-in slide-in-from-bottom-2 duration-300">
                            {payosStatus === 'idle' && (
                                <button
                                    onClick={handleCreatePayosQR}
                                    disabled={payosLoading}
                                    className="w-full py-8 bg-blue-600 text-white rounded-2xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 flex flex-col items-center justify-center gap-3 transition-all"
                                >
                                    {payosLoading ? <Loader2 size={32} className="animate-spin" /> : <QrCode size={32} />}
                                    <span className="text-lg">Tạo mã QR PayOS</span>
                                </button>
                            )}

                            {(payosStatus === 'waiting' || payosStatus === 'expired') && payosData && (
                                <div className="text-center space-y-4 bg-gray-50 p-6 rounded-2xl border-2 border-dashed border-blue-200 relative overflow-hidden">
                                    {payosStatus === 'expired' && (
                                        <div className="absolute inset-0 bg-white/80 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center p-4 text-center">
                                            <div className="bg-red-50 text-red-600 p-3 rounded-full mb-2">
                                                <XCircle size={32} />
                                            </div>
                                            <p className="font-bold text-red-600">Mã thanh toán đã hết hạn</p>
                                            <p className="text-xs text-gray-500 mb-4">Vui lòng tạo lại mã mới để tiếp tục</p>
                                            <button
                                                onClick={handleCreatePayosQR}
                                                className="bg-blue-600 text-white px-6 py-2 rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 flex items-center gap-2 transition-all mx-auto"
                                            >
                                                <RefreshCw size={18} /> Tạo mã mới
                                            </button>
                                        </div>
                                    )}
                                    
                                    <div className="flex justify-center">
                                        <div className="bg-white p-3 rounded-xl shadow-sm border">
                                            <QRCodeSVG
                                                value={payosData.qrCode}
                                                size={220}
                                                level={"H"}
                                                includeMargin={true}
                                            />
                                        </div>
                                    </div>
                                    <div className="space-y-1">
                                        <div className="flex items-center justify-center gap-2 mb-1">
                                            <div className={`px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1.5 ${timeLeft < 60 ? 'bg-red-100 text-red-600 animate-pulse' : 'bg-blue-100 text-blue-600'}`}>
                                                <Loader2 size={12} className={payosStatus === 'waiting' ? "animate-spin" : ""} />
                                                Hết hạn sau: {formatTime(timeLeft)}
                                            </div>
                                        </div>
                                        <p className="font-bold text-gray-700 flex items-center justify-center gap-2">
                                            Đang chờ thanh toán...
                                        </p>
                                        <p className="text-xs text-gray-500">Quét mã bằng ứng dụng Ngân hàng để thanh toán</p>
                                    </div>
                                    <button
                                        onClick={handleCancelPayos}
                                        className="text-red-500 text-sm font-semibold hover:underline flex items-center justify-center gap-1 mx-auto"
                                    >
                                        <XCircle size={14} /> Hủy giao dịch này
                                    </button>
                                </div>
                            )}

                            {payosStatus === 'success' && (
                                <div className="text-center py-8 space-y-3 animate-in zoom-in duration-500">
                                    <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto">
                                        <CheckCircle2 size={48} />
                                    </div>
                                    <h4 className="text-xl font-bold text-green-700">Thanh toán thành công!</h4>
                                    <p className="text-gray-500">Hệ thống đang chuẩn bị in hóa đơn...</p>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="space-y-6 animate-in slide-in-from-bottom-2 duration-300">
                            {/* Voucher Section */}
                            <div className="space-y-3">
                                <label className="text-xs font-bold text-gray-400 uppercase">Ưu đãi & Khuyến mãi</label>
                                <div className="flex gap-2">
                                    <div className="relative flex-1">
                                        <Tag className="absolute left-3 top-3 text-gray-400" size={18} />
                                        <input
                                            type="text"
                                            className="w-full pl-10 pr-4 py-2.5 border rounded-xl uppercase focus:ring-2 focus:ring-orange-500 outline-none text-sm font-semibold"
                                            placeholder="NHẬP MÃ GIẢM GIÁ"
                                            value={voucherCode}
                                            onChange={handleInputChange}
                                        />
                                    </div>
                                    <button
                                        onClick={handleApplyVoucher}
                                        disabled={loading || !voucherCode}
                                        className="px-4 bg-gray-800 text-white rounded-xl font-bold hover:bg-black disabled:opacity-50 transition-colors"
                                    >
                                        Áp dụng
                                    </button>
                                </div>
                                {error && <p className="text-red-500 text-xs font-medium ml-1">{error}</p>}
                                {previewData?.voucherValid && voucherCode && (
                                    <p className="text-green-600 text-xs font-bold ml-1 flex items-center gap-1">
                                        <CheckCircle2 size={12} /> Voucher hợp lệ: -{discount.toLocaleString()}đ
                                    </p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Order Summary Table */}
                    <div className="space-y-3">
                        <label className="text-xs font-bold text-gray-400 uppercase">Chi tiết hóa đơn</label>
                        <div className="bg-gray-50 p-5 rounded-2xl space-y-3 border">
                            <div className="flex justify-between text-sm">
                                <span className="text-gray-500">Tạm tính:</span>
                                <span className="font-semibold text-gray-700">{subTotal.toLocaleString()}đ</span>
                            </div>
                            {discount > 0 && (
                                <div className="flex justify-between text-sm">
                                    <span className="text-green-600 font-medium">Giảm giá voucher:</span>
                                    <span className="font-bold text-green-600">-{discount.toLocaleString()}đ</span>
                                </div>
                            )}
                            <div className="border-t border-dashed pt-3 mt-1 flex justify-between items-center">
                                <span className="font-bold text-gray-800">Cần thanh toán:</span>
                                <span className="text-2xl font-black text-orange-600">{finalTotal.toLocaleString()}đ</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="px-6 py-5 border-t bg-white shrink-0">
                    {paymentMethod === 'CASH' ? (
                        <button
                            onClick={handleConfirmCashPay}
                            disabled={loading}
                            className="w-full py-4 bg-green-600 text-white font-black text-lg rounded-2xl hover:bg-green-700 shadow-xl shadow-green-100 flex items-center justify-center gap-3 transition-all active:scale-[0.98]"
                        >
                            {loading ? <Loader2 className="animate-spin" /> : <CreditCard size={24} />}
                            XÁC NHẬN THANH TOÁN
                        </button>
                    ) : (
                        <button
                            onClick={onClose}
                            className="w-full py-3 text-gray-500 font-bold hover:bg-gray-100 rounded-xl transition-colors"
                        >
                            Quay lại
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PaymentModal;