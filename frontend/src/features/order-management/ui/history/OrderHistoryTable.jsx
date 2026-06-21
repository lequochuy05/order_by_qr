import { Search } from 'lucide-react';

import OrderHistoryRow from './OrderHistoryRow.jsx';

const OrderHistoryTable = ({ orders, loading, onSelect, onPrint, onReconcile }) => (
  <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left">
        <thead>
          <tr className="border-b border-gray-100 bg-gray-50 text-xs uppercase tracking-wider text-gray-500">
            {['Mã ĐH', 'Thời gian', 'Bàn', 'Tổng tiền', 'Trạng thái', 'Thao tác'].map((label) => (
              <th
                key={label}
                className={`p-5 font-semibold ${label === 'Thao tác' ? 'text-right' : ''}`}
              >
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="text-sm">
          {loading ? (
            <tr>
              <td colSpan="6" className="p-12 text-center text-gray-500">
                <div className="flex flex-col items-center justify-center gap-3">
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-orange-500" />
                  <p>Đang tải dữ liệu...</p>
                </div>
              </td>
            </tr>
          ) : orders.length === 0 ? (
            <tr>
              <td colSpan="6" className="p-12 text-center text-gray-500">
                <div className="flex flex-col items-center justify-center gap-2">
                  <Search size={32} className="mb-2 text-gray-300" />
                  <p>Không tìm thấy đơn hàng nào phù hợp.</p>
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
