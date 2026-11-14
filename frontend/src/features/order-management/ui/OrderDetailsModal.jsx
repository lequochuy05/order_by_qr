import { ORDER_STATUSES } from '@entities/order/lib/orderStatus.js';
import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';
import { fmtVND, fmtDateTime } from '@shared/lib/formatters.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from '@entities/order/lib/orderMoney.js';
import { X, Receipt, Clock, Utensils, Printer, RotateCw, UserCheck, CreditCard } from 'lucide-react';
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
      backdropClassName="bg-slate-900/60 backdrop-blur-sm"
      ariaLabel="Chi tiết đơn hàng"
      className="max-w-md !p-0"
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200/80 p-6 transition-colors dark:border-slate-800/80">
        <div className="flex items-center gap-3.5">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
            <Receipt size={20} />
          </div>
          <div>
            <h2 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">
              Chi tiết đơn hàng
            </h2>
            <p className="mt-0.5 text-xs font-semibold text-slate-400 dark:text-slate-500">
              Mã đơn:{' '}
              <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
                #{order.id?.toString().substring(0, 8).toUpperCase() || order.id}
              </span>
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="rounded-xl p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 active:scale-95 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        >
          <X size={20} />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 space-y-5 overflow-y-auto p-6 custom-scrollbar">
        {/* Order Info Summary */}
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-2xl border border-slate-200/80 bg-slate-50/60 p-3.5 dark:border-slate-800/80 dark:bg-slate-900/60">
            <span className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              <Clock size={12} /> Thời gian
            </span>
            <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
              {fmtDateTime(order.createdAt)}
            </span>
          </div>
          <div className="rounded-2xl border border-slate-200/80 bg-slate-50/60 p-3.5 dark:border-slate-800/80 dark:bg-slate-900/60">
            <span className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              <Utensils size={12} /> Bàn
            </span>
            <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
              {order.table?.tableNumber || 'Mang đi'}
            </span>
          </div>
          {order.paidByName && (
            <div className="col-span-2 rounded-2xl border border-indigo-200/60 bg-indigo-50/60 p-3.5 dark:border-indigo-800/40 dark:bg-indigo-950/40">
              <span className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                <UserCheck size={12} /> Thu ngân
              </span>
              <span className="text-xs font-bold uppercase text-indigo-900 dark:text-indigo-200">
                {order.paidByName}
              </span>
            </div>
          )}
          {order.paymentMethod && (
            <div className="col-span-2 rounded-2xl border border-slate-200/80 bg-slate-50/60 p-3.5 dark:border-slate-800/80 dark:bg-slate-900/60">
              <span className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                <CreditCard size={12} /> Hình thức thanh toán
              </span>
              <span className="text-xs font-bold uppercase text-slate-800 dark:text-slate-200">
                {order.paymentMethod === PAYMENT_METHODS.PAYOS ? 'Chuyển khoản (PayOS)' : 'Tiền mặt'}
              </span>
            </div>
          )}
        </div>

        {/* Line Items */}
        <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800/80 dark:bg-slate-900">
          <div className="border-b border-slate-200/80 bg-slate-50/60 px-5 py-3 dark:border-slate-800/80 dark:bg-slate-900/80">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 dark:text-slate-200">
              Danh sách món
            </h3>
          </div>
          <div className="p-5 space-y-3.5 divide-y divide-slate-100 dark:divide-slate-800/60">
            {order.orderItems?.map((item, idx) => (
              <div key={idx} className={`flex justify-between items-start ${idx > 0 ? 'pt-3' : ''}`}>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-800 dark:text-slate-100">
                      {item.itemNameSnapshot ||
                        item.menuItem?.name ||
                        item.combo?.name ||
                        'Món không tên'}
                    </span>
                    <span className="rounded-lg bg-indigo-50 border border-indigo-200/60 px-2 py-0.5 text-[10px] font-bold font-mono text-indigo-700 dark:bg-indigo-950/40 dark:border-indigo-800/40 dark:text-indigo-300">
                      x{item.quantity}
                    </span>
                  </div>
                  {item.notes && (
                    <p className="mt-1 inline-block rounded-md bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
                      {item.notes}
                    </p>
                  )}
                </div>
                <span className="whitespace-nowrap pl-4 font-mono font-bold text-xs text-slate-700 dark:text-slate-300">
                  {fmtVND(item.lineTotal ?? item.unitPrice * item.quantity)}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Financials */}
        <div className="space-y-2.5 rounded-2xl border border-slate-200/80 bg-slate-50/60 p-5 dark:border-slate-800/80 dark:bg-slate-900/60">
          <div className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
            <span>Tổng tiền món</span>
            <span className="font-mono font-bold text-slate-800 dark:text-slate-200">
              {fmtVND(subtotalAmount)}
            </span>
          </div>
          {discountAmount > 0 && (
            <div className="flex justify-between text-emerald-600 text-xs">
              <span>Khuyến mãi</span>
              <span className="font-mono font-bold">-{fmtVND(discountAmount)}</span>
            </div>
          )}
          <div className="mt-2 flex items-center justify-between border-t border-slate-200/80 pt-3 dark:border-slate-800">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-100">THANH TOÁN</span>
            <span className="text-2xl font-black font-mono tracking-tight text-indigo-600 dark:text-indigo-400">
              {fmtVND(finalAmount)}
            </span>
          </div>
        </div>
      </div>

      {/* Footer Actions */}
      <div className="flex gap-3 border-t border-slate-200/80 bg-slate-50/60 p-5 dark:border-slate-800/80 dark:bg-slate-900/60">
        <button
          onClick={() => onReconcile(order.id)}
          className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200/80 bg-white py-3 text-xs font-bold text-slate-700 shadow-sm transition-all hover:bg-slate-50 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
        >
          <RotateCw size={16} />
          <span>Tra soát</span>
        </button>
        {order.status === ORDER_STATUSES.COMPLETED && (
          <button
            onClick={() => onPrint(order)}
            className="flex-1 flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-emerald-600/25 hover:from-emerald-500 hover:to-teal-500 active:scale-95"
          >
            <Printer size={16} />
            <span>In biên lai</span>
          </button>
        )}
      </div>
    </SharedModal>
  );
}
