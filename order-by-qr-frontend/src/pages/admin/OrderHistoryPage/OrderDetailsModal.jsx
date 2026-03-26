import { fmtVND, fmtDateTime } from '../../../utils/formatters';
import { X, Receipt, Clock, Utensils, Printer, Search } from 'lucide-react';


export default function OrderDetailsModal({ isOpen, onClose, order }) {
  if (!isOpen || !order) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-end animate-in fade-in duration-200">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-gray-900/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Drawer Panel */}
      <div className="relative w-full max-w-md h-full bg-white border-l border-gray-200 shadow-2xl flex flex-col animate-in slide-in-from-right duration-300 sm:rounded-l-2xl">

        {/* Header */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center sm:rounded-tl-2xl">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
              <Receipt size={20} />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800 tracking-tight">
                Chi tiết đơn hàng
              </h2>
              <p className="text-gray-500 text-sm mt-0.5">Mã đơn: <span className="text-orange-600 font-mono font-medium">#{order.id?.toString().substring(0, 8).toUpperCase() || order.id}</span></p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-thin scrollbar-thumb-gray-200">

          {/* Order Info Summary */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-2xl p-4 border border-gray-100">
              <span className="block text-xs text-gray-500 font-medium mb-1.5 flex items-center gap-1">
                <Clock size={12} /> THỜI GIAN
              </span>
              <span className="text-gray-800 font-medium text-sm">{fmtDateTime(order.createdAt)}</span>
            </div>
            <div className="bg-gray-50 rounded-2xl p-4 border border-gray-100">
              <span className="block text-xs text-gray-500 font-medium mb-1.5 flex items-center gap-1">
                <Utensils size={12} /> BÀN
              </span>
              <span className="text-gray-800 font-medium text-sm">{order.table?.tableNumber || 'Mang đi'}</span>
            </div>
          </div>

          {/* Line Items */}
          <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm">
            <div className="bg-gray-50 px-5 py-3 border-b border-gray-100">
              <h3 className="text-sm font-semibold text-gray-700">Danh sách món</h3>
            </div>
            <div className="p-5 space-y-4">
              {order.orderItems?.map((item, idx) => (
                <div key={idx} className="flex justify-between items-start group">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-gray-800 font-medium">{item.menuItem?.name || 'Món ăn'}</span>
                      <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-md font-medium">x{item.quantity}</span>
                    </div>
                    {item.notes && (
                      <p className="text-xs text-orange-600 mt-1.5 flex gap-1.5 items-start bg-orange-50 inline-block px-2 py-1 rounded-md">
                        <span className="w-1.5 h-1.5 rounded-full bg-orange-500 mt-1 flex-shrink-0"></span>
                        {item.notes}
                      </p>
                    )}
                  </div>
                  <span className="text-gray-700 font-medium whitespace-nowrap pl-4">{fmtVND(item.unitPrice * item.quantity)}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Financials */}
          <div className="bg-gray-50 rounded-2xl p-5 border border-gray-100 space-y-3">
            <div className="flex justify-between text-gray-600 text-sm">
              <span>Tổng tiền món</span>
              <span className="font-medium text-gray-800">{fmtVND(order.originalTotal || 0)}</span>
            </div>
            {order.discountVoucher > 0 && (
              <div className="flex justify-between text-emerald-600 text-sm">
                <span>Khuyến mãi</span>
                <span className="font-medium">-{fmtVND(order.discountVoucher)}</span>
              </div>
            )}
            <div className="pt-4 mt-2 border-t border-gray-200 flex justify-between items-center">
              <span className="text-gray-800 font-bold">THANH TOÁN</span>
              <span className="text-xl font-bold text-orange-600">{fmtVND(order.totalAmount || 0)}</span>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-6 border-t border-gray-100 bg-gray-50 sm:rounded-bl-2xl flex gap-3">
          <button className="flex-1 flex items-center justify-center gap-2 py-3 bg-white hover:bg-gray-50 text-gray-700 font-medium rounded-xl transition-all border border-gray-200 shadow-sm">
            <Search size={18} />
            <span>Tra soát</span>
          </button>
          <button className="flex-1 flex items-center justify-center gap-2 py-3 bg-orange-500 hover:bg-orange-600 text-white font-medium rounded-xl transition-all shadow-md shadow-orange-500/20">
            <Printer size={18} />
            <span>In biên lai</span>
          </button>
        </div>

      </div>
    </div>
  );
}

// const Clock = ({ size = 24, ...props }) => (
//   <svg
//     xmlns="http://www.w3.org/2000/svg"
//     width={size}
//     height={size}
//     viewBox="0 0 24 24"
//     fill="none"
//     stroke="currentColor"
//     strokeWidth="2"
//     strokeLinecap="round"
//     strokeLinejoin="round"
//     {...props}
//   >
//     <circle cx="12" cy="12" r="10" />
//     <polyline points="12 6 12 12 16 14" />
//   </svg>
// );
