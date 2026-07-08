import { ORDER_STATUSES } from '@entities/order/lib/orderStatus.js';
import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';
import { fmtVND, fmtDateTime } from '@shared/lib/formatters.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { X, Receipt, Clock, Utensils, Printer, RotateCw } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';

export default function OrderDetailsModal({ isOpen, onClose, order, onPrint, onReconcile }) {
  if (!isOpen || !order) return null;
  const subtotalAmount = getOrderSubtotalAmount(order);
  const discountAmount = getOrderDiscountAmount(order);
  const finalAmount = getOrderFinalAmount(order);

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      placement="right"
      backdropClassName="bg-gray-900/40 backdrop-blur-sm"
      ariaLabel="Chi tiết đơn hàng"
      className="max-w-md"
    >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 p-6 transition-colors dark:border-slate-800 sm:rounded-tl-2xl">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300">
              <Receipt size={20} />
            </div>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                Chi tiết đơn hàng
              </h2>
              <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
                Mã đơn:{' '}
                <span className="font-mono font-medium text-orange-600 dark:text-orange-300">
                  #{order.id?.toString().substring(0, 8).toUpperCase() || order.id}
                </span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-xl p-2 text-slate-400 transition-colors hover:bg-slate-50 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 space-y-6 overflow-y-auto p-6 scrollbar-thin scrollbar-thumb-slate-200 dark:scrollbar-thumb-slate-700">
          {/* Order Info Summary */}
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950/60">
              <span className="mb-1.5 flex items-center gap-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                <Clock size={12} /> THỜI GIAN
              </span>
              <span className="text-sm font-medium text-slate-800 dark:text-slate-200">
                {fmtDateTime(order.createdAt)}
              </span>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950/60">
              <span className="mb-1.5 flex items-center gap-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                <Utensils size={12} /> BÀN
              </span>
              <span className="text-sm font-medium text-slate-800 dark:text-slate-200">
                {order.table?.tableNumber}
              </span>
            </div>
            {order.paidByName && (
              <div className="col-span-2 rounded-2xl border border-orange-100 bg-orange-50 p-4 dark:border-orange-500/30 dark:bg-orange-500/10">
                <span className="mb-1.5 flex items-center gap-1 text-xs font-medium text-orange-600 dark:text-orange-300">
                  THU NGÂN
                </span>
                <span className="text-sm font-bold uppercase text-orange-900 dark:text-orange-200">
                  {order.paidByName}
                </span>
              </div>
            )}
            {order.paymentMethod && (
              <div className="col-span-2 rounded-2xl border border-indigo-100 bg-indigo-50 p-4 dark:border-indigo-500/30 dark:bg-indigo-500/10">
                <span className="mb-1.5 flex items-center gap-1 text-xs font-medium text-indigo-600 dark:text-indigo-300">
                  HÌNH THỨC THANH TOÁN
                </span>
                <span className="text-sm font-bold uppercase text-indigo-900 dark:text-indigo-200">
                  {order.paymentMethod === PAYMENT_METHODS.PAYOS ? 'Chuyển khoản (PayOS)' : 'Tiền mặt'}
                </span>
              </div>
            )}
          </div>

          {/* Line Items */}
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="border-b border-slate-100 bg-slate-50 px-5 py-3 dark:border-slate-800 dark:bg-slate-950/60">
              <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                Danh sách món
              </h3>
            </div>
            <div className="p-5 space-y-4">
              {order.orderItems?.map((item, idx) => (
                <div key={idx} className="flex justify-between items-start group">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-800 dark:text-slate-100">
                        {item.itemNameSnapshot ||
                          item.menuItem?.name ||
                          item.combo?.name ||
                          'Món không tên'}
                      </span>
                      <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                        x{item.quantity}
                      </span>
                    </div>
                    {item.notes && (
                      <p className="mt-1.5 inline-block rounded-md bg-orange-50 px-2 py-1 text-xs text-orange-600 dark:bg-orange-500/10 dark:text-orange-300">
                        <span className="w-1.5 h-1.5 rounded-full bg-orange-500 mt-1 flex-shrink-0"></span>
                        {item.notes}
                      </p>
                    )}
                  </div>
                  <span className="whitespace-nowrap pl-4 font-medium text-slate-700 dark:text-slate-300">
                    {fmtVND(item.lineTotal ?? item.unitPrice * item.quantity)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Financials */}
          <div className="space-y-3 rounded-2xl border border-slate-100 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950/60">
            <div className="flex justify-between text-sm text-slate-600 dark:text-slate-400">
              <span>Tổng tiền món</span>
              <span className="font-medium text-slate-800 dark:text-slate-200">
                {fmtVND(subtotalAmount)}
              </span>
            </div>
            {discountAmount > 0 && (
              <div className="flex justify-between text-emerald-600 text-sm">
                <span>Khuyến mãi</span>
                <span className="font-medium">-{fmtVND(discountAmount)}</span>
              </div>
            )}
            <div className="mt-2 flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800">
              <span className="font-bold text-slate-800 dark:text-slate-100">THANH TOÁN</span>
              <span className="text-xl font-bold text-orange-600 dark:text-orange-300">
                {fmtVND(finalAmount)}
              </span>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex gap-3 border-t border-slate-100 bg-slate-50 p-6 dark:border-slate-800 dark:bg-slate-950/60 sm:rounded-bl-2xl">
          <button
            onClick={() => onReconcile(order.id)}
            className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white py-3 font-medium text-slate-700 shadow-sm transition-all hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            <RotateCw size={18} />
            <span>Tra soát</span>
          </button>
          {order.status === ORDER_STATUSES.COMPLETED && (
            <button
              onClick={() => onPrint(order)}
              className="flex-1 flex items-center justify-center gap-2 py-3 bg-orange-500 hover:bg-orange-600 text-white font-medium rounded-xl transition-all shadow-md shadow-orange-500/20"
            >
              <Printer size={18} />
              <span>In biên lai</span>
            </button>
          )}
        </div>
    </SharedModal>
  );
}
