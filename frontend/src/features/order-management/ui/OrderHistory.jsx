import useOrderHistory from '@features/order-management/model/useOrderHistory.js';
import { ErrorBoundary } from '@shared/ui';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import OrderDetailsModal from './OrderDetailsModal.jsx';
import OrderHistoryFilters from './history/OrderHistoryFilters.jsx';
import OrderHistoryStats from './history/OrderHistoryStats.jsx';
import OrderHistoryTable from './history/OrderHistoryTable.jsx';

const OrderHistoryContent = () => {
  const history = useOrderHistory();

  return (
    <div className="mx-auto w-full min-w-0 max-w-7xl space-y-4 p-0 animate-in fade-in duration-500 sm:space-y-6 sm:p-3 lg:p-6">
      <OrderHistoryStats analytics={history.analytics} averageOrder={history.averageOrder} />
      <OrderHistoryFilters
        orderId={history.orderId}
        tableNumber={history.tableNumber}
        status={history.status}
        datePreset={history.datePreset}
        customStartDate={history.customStartDate}
        customEndDate={history.customEndDate}
        loading={history.loading}
        onOrderIdChange={history.setOrderId}
        onTableNumberChange={history.setTableNumber}
        onStatusChange={history.setStatus}
        onDatePresetChange={history.setDatePreset}
        onCustomStartDateChange={history.setCustomStartDate}
        onCustomEndDateChange={history.setCustomEndDate}
        onApply={history.applyFilters}
        onRefresh={history.refreshData}
      />
      <OrderHistoryTable
        orders={history.orders}
        loading={history.loading}
        onSelect={history.setSelectedOrder}
        onPrint={history.printOrder}
        onReconcile={history.reconcileOrder}
      />
      <PaginationControls
        currentPage={history.currentPage}
        totalPages={history.totalPages}
        totalElements={history.totalElements}
        itemLabel="đơn hàng"
        loading={history.loading}
        onPageChange={history.setCurrentPage}
      />
      <OrderDetailsModal
        isOpen={Boolean(history.selectedOrder)}
        onClose={() => history.setSelectedOrder(null)}
        order={history.selectedOrder}
        onPrint={history.printOrder}
        onReconcile={history.reconcileOrder}
      />
    </div>
  );
};

const OrderHistory = () => (
  <ErrorBoundary>
    <OrderHistoryContent />
  </ErrorBoundary>
);

export default OrderHistory;
