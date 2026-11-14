import { Search } from 'lucide-react';

import OrderHistoryRow from './OrderHistoryRow.jsx';

const OrderHistoryTable = ({ orders, loading, onSelect, onPrint, onReconcile }) => (
  <div className="overflow-hidden rounded-3xl border border-[#1E293B] bg-white/90 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left">
        <thead>
          <tr className="border-b border-[#1E293B]/20 bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:border-[#1E293B] dark:bg-slate-950/80 dark:text-slate-400">
            {['Mã ĐH', 'Thời gian', 'Bàn', 'Tổng tiền', 'Trạng thái', 'Thao tác'].map((label) => (
              <th
                key={label}
                className={`p-5 font-bold ${label === 'Thao tác' ? 'text-right' : ''}`}
              >
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="text-sm">
          {loading ? (
            <tr>
              <td colSpan="6" className="p-16 text-center text-slate-500 dark:text-slate-400">
                <div className="flex flex-col items-center justify-center gap-3">
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-indigo-600 dark:border-slate-800 dark:border-t-indigo-400" />
                  <p className="text-xs font-semibold">Đang tải dữ liệu...</p>
                </div>
              </td>
            </tr>
          ) : orders.length === 0 ? (
            <tr>
              <td colSpan="6" className="p-16 text-center text-slate-500 dark:text-slate-400">
                <div className="flex flex-col items-center justify-center gap-2">
                  <Search size={36} className="mb-2 text-slate-300 dark:text-slate-600" />
                  <p className="text-sm italic">Không tìm thấy đơn hàng nào phù hợp.</p>
                </div>
              </td>
            </tr>
          ) : (
            orders.map((order) => (
              <OrderHistoryRow
                key={order.id}
                order={order}
                onSelect={onSelect}
                onPrint={onPrint}
                onReconcile={onReconcile}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

export default OrderHistoryTable;
