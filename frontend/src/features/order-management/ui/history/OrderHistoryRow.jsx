import { Eye, Printer, RotateCw } from 'lucide-react';

import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import { fmtDateTime, fmtVND } from '@shared/lib/formatters.js';

const OrderHistoryRow = ({ order, onSelect, onPrint, onReconcile }) => (
  <tr
    className="group cursor-pointer border-b border-gray-50 transition-colors hover:bg-gray-50"
    onClick={() => onSelect(order)}
  >
    <td className="p-5 font-mono font-medium text-gray-800">
      <span className="mr-1 text-gray-400">#</span>
      {order.id?.toString().substring(0, 8).toUpperCase()}
    </td>
    <td className="p-5 text-gray-600">{fmtDateTime(order.createdAt)}</td>
    <td className="p-5">
      {order.table?.tableNumber || order.table?.name ? (
        <span className="rounded-lg bg-gray-100 px-3 py-1.5 font-medium text-gray-700">
          {order.table.tableNumber || order.table.name}
        </span>
      ) : (
        <span className="italic text-gray-500">Mang đi</span>
      )}
    </td>
    <td className="p-5 font-semibold text-gray-800">{fmtVND(getOrderFinalAmount(order))}</td>
    <td className="p-5">
      <span
        className={`rounded-full border px-3 py-1 text-xs font-medium ${
          getOrderStatusMeta(order.status).classes
        }`}
      >
        {getOrderStatusMeta(order.status).label}
      </span>
    </td>
    <td className="flex justify-end gap-1 p-5 text-right">
      <button
        type="button"
        className="rounded-xl p-2 text-gray-400 opacity-0 transition-all hover:bg-blue-50 hover:text-blue-500 group-hover:opacity-100"
        title="Tra soát thanh toán"
        onClick={(event) => {
          event.stopPropagation();
          onReconcile(order.id);
        }}
      >
        <RotateCw size={18} />
      </button>
      {order.status === 'COMPLETED' && (
        <button
          type="button"
          className="rounded-xl p-2 text-gray-400 opacity-0 transition-all hover:bg-green-50 hover:text-green-500 group-hover:opacity-100"
          title="In hóa đơn"
          onClick={(event) => {
            event.stopPropagation();
            onPrint(order);
          }}
        >
          <Printer size={18} />
        </button>
      )}
      <button
        type="button"
        className="rounded-xl p-2 text-gray-400 opacity-0 transition-all hover:bg-orange-50 hover:text-orange-500 group-hover:opacity-100"
        title="Xem chi tiết"
        onClick={(event) => {
          event.stopPropagation();
          onSelect(order);
        }}
      >
        <Eye size={18} />
      </button>
    </td>
  </tr>
);

export default OrderHistoryRow;
