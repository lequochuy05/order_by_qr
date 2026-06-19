import { useState, useCallback } from 'react';
import {
  RefreshCcw,
  Search,
  Filter,
  Eye,
  Calendar,
  TrendingUp,
  ShoppingBag,
  DollarSign,
  Hash,
  Printer,
  RotateCw,
} from 'lucide-react';
import { fmtVND, fmtDateTime } from '@shared/lib/formatters.js';
import { ORDER_STATUS, getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@shared/lib/orderMoney.js';
import { printInvoice } from '@shared/lib/invoiceGenerator.js';
import OrderDetailsModal from './OrderDetailsModal';
import { toast } from 'react-hot-toast';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { useOrdersHistoryQuery, useOrderAnalyticsQuery } from '../api/orderQueries.js';
import { useReconcileOrderMutation } from '../api/orderMutations.js';
import { ErrorBoundary } from '@shared/ui';

const DATE_PRESETS = [
  { label: 'Tất cả', value: 'all' },
  { label: 'Hôm nay', value: 'today' },
  { label: 'Hôm qua', value: 'yesterday' },
  { label: '7 ngày', value: '7days' },
  { label: '30 ngày', value: '30days' },
];

function getDateRange(preset) {
  const today = new Date();
  const fmt = (d) => {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  switch (preset) {
    case 'today':
      return { startDate: fmt(today), endDate: fmt(today) };
    case 'yesterday': {
      const y = new Date(today);
      y.setDate(y.getDate() - 1);
      return { startDate: fmt(y), endDate: fmt(y) };
    }
    case '7days': {
      const d = new Date(today);
      d.setDate(d.getDate() - 6);
      return { startDate: fmt(d), endDate: fmt(today) };
    }
    case '30days': {
      const d = new Date(today);
      d.setDate(d.getDate() - 29);
      return { startDate: fmt(d), endDate: fmt(today) };
    }
    default:
      return { startDate: '', endDate: '' };
  }
}

function OrderHistoryContent() {
  const [selectedOrder, setSelectedOrder] = useState(null);

  // Server-side pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 10;

  // Filters
  const [orderId, setOrderId] = useState('');
  const [tableNumber, setTableNumber] = useState('');
  const [status, setStatus] = useState('');
  const [datePreset, setDatePreset] = useState('today');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  // Applied filters (only update when user clicks "Lọc")
  const [appliedFilters, setAppliedFilters] = useState(() => {
    const range = getDateRange('today');
    return {
      orderId: '',
      tableNumber: '',
      status: '',
      datePreset: 'today',
      startDate: range.startDate,
      endDate: range.endDate,
    };
  });

  const getFilterParams = useCallback(() => {
    return {
      ...(appliedFilters.orderId && { orderId: appliedFilters.orderId }),
      ...(appliedFilters.tableNumber && { tableNumber: appliedFilters.tableNumber }),
      ...(appliedFilters.status && { status: appliedFilters.status }),
      ...(appliedFilters.startDate && { from: appliedFilters.startDate }),
      ...(appliedFilters.endDate && { to: appliedFilters.endDate }),
    };
  }, [appliedFilters]);

  const handleApplyFilters = () => {
    let dateRange;
    if (datePreset === 'custom') {
      dateRange = { startDate: customStartDate, endDate: customEndDate };
    } else {
      dateRange = getDateRange(datePreset);
    }

    setAppliedFilters({
      orderId: orderId.trim(),
      tableNumber: tableNumber.trim(),
      status,
      datePreset,
      startDate: dateRange.startDate,
      endDate: dateRange.endDate,
    });
    setCurrentPage(0); // Reset to first page on new filter
  };

  const filterParams = getFilterParams();

  const queryParams = {
    ...filterParams,
    page: currentPage,
    size: itemsPerPage,
  };

  const { data: ordersData, isFetching: loadingOrders } = useOrdersHistoryQuery(queryParams);
  const { data: analyticsData, isFetching: loadingAnalytics } =
    useOrderAnalyticsQuery(filterParams);

  const loading = loadingOrders || loadingAnalytics;
  const orders = ordersData?.content || [];
  const totalPages = ordersData?.totalPages || 0;
  const totalElements = ordersData?.totalElements || 0;
  const analytics = analyticsData || { totalOrders: 0, totalRevenue: 0 };

  const refreshData = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.orders.history() });
    queryClient.invalidateQueries({ queryKey: ['orders', 'analytics'] });
  };

  const avgOrder = analytics.totalOrders > 0 ? analytics.totalRevenue / analytics.totalOrders : 0;

  const handlePrint = (e, order) => {
    if (e && e.stopPropagation) e.stopPropagation();

    if (order.status !== 'COMPLETED') {
      toast.error('Chỉ có thể in hóa đơn cho đơn hàng đã hoàn tất');
      return;
    }

    printInvoice({
      order,
      table: order.table,
      paidBy: order.paidByName || 'Nhân viên',
      paidAt: order.paymentTime,
    });
  };

  const reconcileMutation = useReconcileOrderMutation();

  const handleReconcile = async (e, orderId) => {
    e.stopPropagation();
    try {
      const reconciledOrder = await reconcileMutation.mutateAsync(orderId);
      toast.success(`Tra soát thành công: #${reconciledOrder?.id || orderId}`);
    } catch (error) {
      toast.error('Lỗi khi tra soát đơn hàng: ' + (error?.message || ''));
    }
  };

  return (
    <div className="mx-auto w-full min-w-0 max-w-7xl space-y-4 p-0 animate-in fade-in duration-500 sm:space-y-6 sm:p-3 lg:p-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-4">
          <div className="p-3 bg-blue-50 rounded-xl">
            <ShoppingBag size={22} className="text-blue-500" />
          </div>
          <div>
            <p className="text-xs text-gray-500 font-medium uppercase tracking-wider">
              Tổng đơn hàng
            </p>
            <p className="text-2xl font-bold text-gray-800">
              {analytics.totalOrders?.toLocaleString() || 0}
            </p>
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-4">
          <div className="p-3 bg-green-50 rounded-xl">
            <DollarSign size={22} className="text-green-500" />
          </div>
          <div>
            <p className="text-xs text-gray-500 font-medium uppercase tracking-wider">
              Tổng doanh thu
            </p>
            <p className="text-2xl font-bold text-gray-800">
              {fmtVND(analytics.totalRevenue || 0)}
            </p>
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-4">
          <div className="p-3 bg-orange-50 rounded-xl">
            <TrendingUp size={22} className="text-orange-500" />
          </div>
          <div>
            <p className="text-xs text-gray-500 font-medium uppercase tracking-wider">TB/Đơn</p>
            <p className="text-2xl font-bold text-gray-800">{fmtVND(avgOrder) || fmtVND(0)}</p>
          </div>
        </div>
      </div>

      {/* Header + Refresh */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        {/* Date Preset Tabs */}
        <div className="flex items-center gap-2 flex-wrap">
          {DATE_PRESETS.map((p) => (
            <button
              key={p.value}
              onClick={() => setDatePreset(p.value)}
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border
                ${
                  datePreset === p.value
                    ? 'bg-orange-500 text-white border-orange-500 shadow-md shadow-orange-200'
                    : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
            >
              {p.label}
            </button>
          ))}
          <button
            onClick={() => setDatePreset('custom')}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border flex items-center gap-1.5
              ${
                datePreset === 'custom'
                  ? 'bg-orange-500 text-white border-orange-500 shadow-md shadow-orange-200'
                  : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
              }`}
          >
            <Calendar size={14} /> Tùy chọn
          </button>

          {/* Apply Button */}
          <button
            onClick={handleApplyFilters}
            className="px-4 py-2 rounded-xl text-sm font-medium transition-all border flex items-center gap-1.5 bg-indigo-500 text-white border-indigo-500 shadow-md shadow-indigo-200"
          >
            <Filter size={18} />
            <span>Áp dụng</span>
          </button>
        </div>

        <button
          onClick={() => refreshData({ force: true })}
          className="flex items-center gap-2 px-5 py-2.5 bg-white hover:bg-gray-50 text-gray-700 rounded-xl transition-all shadow-sm border border-gray-200 font-medium"
        >
          <RefreshCcw
            size={18}
            className={loading ? 'animate-spin text-orange-500' : 'text-gray-400'}
          />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Custom Date Range */}
      {datePreset === 'custom' && (
        <div className="flex items-center gap-3 p-4 bg-white rounded-2xl border border-gray-100 shadow-sm">
          <Calendar size={18} className="text-gray-400" />
          <input
            type="date"
            value={customStartDate}
            onChange={(e) => setCustomStartDate(e.target.value)}
            className="px-3 py-2 bg-gray-50 rounded-xl border border-gray-200 text-sm outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
          />
          <span className="text-gray-400">→</span>
          <input
            type="date"
            value={customEndDate}
            onChange={(e) => setCustomEndDate(e.target.value)}
            className="px-3 py-2 bg-gray-50 rounded-xl border border-gray-200 text-sm outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
          />
        </div>
      )}

      {/* Filters bar */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-4 p-5 bg-white rounded-2xl border border-gray-100 shadow-sm">
        {/* Order ID */}
        <div className="relative group md:col-span-3">
          <Search
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-orange-500 transition-colors"
            size={18}
          />
          <input
            type="text"
            placeholder="Mã ĐH..."
            value={orderId}
            onChange={(e) => setOrderId(e.target.value)}
            className="w-full pl-11 pr-4 py-3 bg-white text-gray-800 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all placeholder:text-gray-400 text-sm"
          />
        </div>

        {/* Table Number */}
        <div className="relative group md:col-span-3">
          <Hash
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-orange-500 transition-colors"
            size={18}
          />
          <input
            type="text"
            placeholder="Số bàn..."
            value={tableNumber}
            onChange={(e) => setTableNumber(e.target.value)}
            className="w-full pl-11 pr-4 py-3 bg-white text-gray-800 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all placeholder:text-gray-400 text-sm"
          />
        </div>

        {/* Status */}
        <div className="relative group md:col-span-4">
          <Filter
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-orange-500 transition-colors"
            size={18}
          />
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="w-full pl-11 pr-4 py-3 bg-white text-gray-800 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all appearance-none text-sm cursor-pointer"
          >
            <option value="">Tất cả trạng thái</option>
            {Object.keys(ORDER_STATUS).map((key) => (
              <option key={key} value={key}>
                {getOrderStatusMeta(key).label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100 text-xs uppercase tracking-wider text-gray-500">
                <th className="p-5 font-semibold">Mã ĐH</th>
                <th className="p-5 font-semibold">Thời gian</th>
                <th className="p-5 font-semibold">Bàn</th>
                <th className="p-5 font-semibold">Tổng tiền</th>
                <th className="p-5 font-semibold">Trạng thái</th>
                <th className="p-5 font-semibold text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="text-sm">
              {loading ? (
                <tr key="loading-row">
                  <td colSpan="6" className="p-12 text-center text-gray-500">
                    <div className="flex flex-col items-center justify-center gap-3">
                      <div className="w-8 h-8 border-4 border-gray-200 border-t-orange-500 rounded-full animate-spin"></div>
                      <p>Đang tải dữ liệu...</p>
                    </div>
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr key="empty-history-row">
                  <td colSpan="6" className="p-12 text-center text-gray-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <Search size={32} className="text-gray-300 mb-2" />
                      <p>Không tìm thấy đơn hàng nào phù hợp.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                orders.map((order, idx) => (
                  <tr
                    key={`order-${order.id || idx}`}
                    className="border-b border-gray-50 hover:bg-gray-50 transition-colors group cursor-pointer"
                    onClick={() => setSelectedOrder(order)}
                  >
                    <td className="p-5 text-gray-800 font-mono font-medium">
                      <span className="text-gray-400 mr-1">#</span>
                      {order.id?.toString().substring(0, 8).toUpperCase()}
                    </td>
                    <td className="p-5 text-gray-600">{fmtDateTime(order.createdAt)}</td>
                    <td className="p-5">
                      {order.table?.tableNumber || order.table?.name ? (
                        <span className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-lg font-medium">
                          {order.table.tableNumber || order.table.name}
                        </span>
                      ) : (
                        <span className="text-gray-500 italic">Mang đi</span>
                      )}
                    </td>
                    <td className="p-5 text-gray-800 font-semibold">
                      {fmtVND(getOrderFinalAmount(order))}
                    </td>
                    <td className="p-5">
                      <span
                        className={`px-3 py-1 text-xs font-medium rounded-full border ${getOrderStatusMeta(order.status).classes}`}
                      >
                        {getOrderStatusMeta(order.status).label}
                      </span>
                    </td>
                    <td className="p-5 text-right flex justify-end gap-1">
                      <button
                        className="p-2 text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded-xl transition-all opacity-0 group-hover:opacity-100"
                        title="Tra soát thanh toán"
                        onClick={(e) => handleReconcile(e, order.id)}
                      >
                        <RotateCw size={18} />
                      </button>
                      {order.status === 'COMPLETED' && (
                        <button
                          className="p-2 text-gray-400 hover:text-green-500 hover:bg-green-50 rounded-xl transition-all opacity-0 group-hover:opacity-100"
                          title="In hóa đơn"
                          onClick={(e) => handlePrint(e, order)}
                        >
                          <Printer size={18} />
                        </button>
                      )}
                      <button
                        className="p-2 text-gray-400 hover:text-orange-500 hover:bg-orange-50 rounded-xl transition-all opacity-0 group-hover:opacity-100"
                        title="Xem chi tiết"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedOrder(order);
                        }}
                      >
                        <Eye size={18} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-3 pt-2">
          <button
            disabled={currentPage === 0}
            onClick={() => setCurrentPage((prev) => prev - 1)}
            className="px-4 py-2 rounded-xl border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors shadow-sm font-medium"
          >
            Trước
          </button>
          <span className="text-gray-500 font-medium px-4">
            Trang {currentPage + 1} / {totalPages}
            <span className="text-gray-400 ml-2 text-sm">({totalElements} đơn)</span>
          </span>
          <button
            disabled={currentPage >= totalPages - 1}
            onClick={() => setCurrentPage((prev) => prev + 1)}
            className="px-4 py-2 rounded-xl border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors shadow-sm font-medium"
          >
            Sau
          </button>
        </div>
      )}

      <OrderDetailsModal
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        order={selectedOrder}
        onPrint={(order) => handlePrint({ stopPropagation: () => {} }, order)}
        onReconcile={(orderId) => handleReconcile({ stopPropagation: () => {} }, orderId)}
      />
    </div>
  );
}

export default function OrderHistory() {
  return (
    <ErrorBoundary>
      <OrderHistoryContent />
    </ErrorBoundary>
  );
}
