import React, { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useAuth } from '@features/auth';

import { orderService } from '@entities/order/api/orderService.js';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { tableOrderCatalogService } from '@entities/public-menu';
import {
  useCreateTableMutation,
  useUpdateTableMutation,
  useUpdateTableStatusMutation,
  useDeleteTableMutation,
  useRegenerateQrMutation,
  useMergeTablesMutation,
  useTableBoardQuery,
} from '@entities/table';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import TableFormModal from './TableFormModal.jsx';
import TableGrid from './TableGrid.jsx';
import OrderDetailModal from './OrderDetailModal.jsx';
import AddItemModal from './AddItemModal.jsx';
import PaymentModal from './PaymentModal.jsx';
import MergeTableModal from './MergeTableModal.jsx';
import { ORDER_STATUSES } from '@entities/order/lib/orderStatus.js';
import { TABLE_STATUS, TABLE_STATUSES } from '@entities/table/lib/tableStatus.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const tableStatusFilterOptions = Object.entries(TABLE_STATUS).map(([id, meta]) => ({
  id,
  name: meta.label,
}));

const TableManager = () => {
  const [filter, setFilter] = useState('ALL');

  // Modal States
  const [formModal, setFormModal] = useState({ open: false, data: null });
  const [detailModal, setDetailModal] = useState({ open: false, table: null, order: null });
  const [addItemModal, setAddItemModal] = useState({ open: false, table: null });
  const [payModal, setPayModal] = useState({ open: false, table: null, order: null });
  const [mergeModal, setMergeModal] = useState({ open: false, table: null });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRegeneratingQr, setIsRegeneratingQr] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const { showError } = useStatusModal();
  const { confirm } = useConfirmModal();

  const { user } = useAuth();
  const userRole = user?.role || 'STAFF';
  const canManageTables = userRole === 'ADMIN' || userRole === 'MANAGER';

  const checkPermission = () => {
    if (!canManageTables) {
      showError('Bạn không có quyền thực hiện chức năng này!');
      return false;
    }
    return true;
  };

  const { data: board, isLoading: loading } = useTableBoardQuery();

  React.useEffect(() => {
    if (loading) return undefined;
    const timeout = window.setTimeout(() => {
      tableOrderCatalogService.getCatalog().catch(() => {});
    }, 800);
    return () => window.clearTimeout(timeout);
  }, [loading]);

  const tables = React.useMemo(() => {
    const tableData = Array.isArray(board?.tables) ? board.tables : [];
    return [...tableData].sort((a, b) =>
      String(a.tableNumber).localeCompare(String(b.tableNumber), undefined, { numeric: true }),
    );
  }, [board?.tables]);

  const orders = React.useMemo(() => {
    const activeOrders = Array.isArray(board?.activeOrders) ? board.activeOrders : [];
    const newOrdersMap = {};
    activeOrders.forEach((ord) => {
      if (ord.tableId && !newOrdersMap[ord.tableId]) {
        newOrdersMap[ord.tableId] = {
          id: ord.id,
          status: ord.status,
          finalAmount: ord.finalAmount,
          createdAt: ord.createdAt,
          table: {
            id: ord.tableId,
            tableNumber: ord.tableNumber,
          },
        };
      }
    });
    return newOrdersMap;
  }, [board?.activeOrders]);

  const createMutation = useCreateTableMutation();
  const updateMutation = useUpdateTableMutation();
  const updateStatusMutation = useUpdateTableStatusMutation();
  const deleteMutation = useDeleteTableMutation();
  const regenerateQrMutation = useRegenerateQrMutation();
  const mergeMutation = useMergeTablesMutation();

  // 3. Handlers
  const handleAction = async ({ type, table, order }) => {
    switch (type) {
      case 'ADD_ITEM':
        setAddItemModal({ open: true, table });
        break;
      case 'DETAIL': {
        const fullOrder = order?.id ? await orderService.getCurrentOrder(table.id) : null;
        setDetailModal({ open: true, table, order: fullOrder || null });
        break;
      }
      case 'PAY':
        try {
          if (!order) {
            showError('Không tìm thấy đơn đang hoạt động của bàn này');
            queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
            break;
          }
          if (order.status !== ORDER_STATUSES.AWAITING_PAYMENT) {
            showError('Vui lòng hoàn thành các món trước khi thanh toán');
            break;
          }
          const fullOrder = await orderService.getOrderById(order.id);
          setPayModal({ open: true, table, order: fullOrder || order });
        } catch (error) {
          showError(error);
        }
        break;
      case 'MERGE':
        setMergeModal({ open: true, table });
        break;
      case 'EDIT':
        if (checkPermission()) {
          setFormErrors({});
          setFormModal({ open: true, data: table });
        }
        break;
      case 'DELETE':
        if (checkPermission()) handleDeleteTable(table.id);
        break;
      default:
        break;
    }
  };

  const handleSaveTable = async (data) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      if (data.id) {
        const { status, initialStatus, ...tableData } = data;
        const updatedTable = await updateMutation.mutateAsync({ id: data.id, data: tableData });
        if (status && status !== initialStatus) {
          await updateStatusMutation.mutateAsync({
            id: data.id,
            status,
            version: updatedTable?.version ?? data.version,
          });
        }
      } else {
        await createMutation.mutateAsync(data);
      }
      showSuccessToast(data.id ? 'Cập nhật bàn thành công' : 'Thêm bàn thành công');
      setFormErrors({});
      setFormModal({ open: false, data: null });
    } catch (e) {
      const errorMsg = e.message || '';
      if (e?.code === 'TABLE_NUMBER_EXISTS' || errorMsg.includes('Table number already exists')) {
        setFormErrors({ tableNumber: 'Số bàn này đã tồn tại' });
      } else {
        showErrorToast(e);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteTable = async (id) => {
    const confirmed = await confirm('Xóa bàn', 'Bạn có chắc chắn muốn xóa bàn này?');
    if (!confirmed) return;
    try {
      await deleteMutation.mutateAsync(id);
      showSuccessToast('Đã xóa bàn');
    } catch (e) {
      showErrorToast(e);
    }
  };

  const handleRegenerateQr = async (id) => {
    if (isRegeneratingQr) return null;
    const confirmed = await confirm(
      'Tạo lại mã QR',
      'Mã QR cũ của bàn này sẽ không còn dùng được. Bạn có chắc chắn muốn xóa QR cũ và tạo mã QR mới?',
    );
    if (!confirmed) return null;

    setIsRegeneratingQr(true);
    try {
      const updatedTable = await regenerateQrMutation.mutateAsync(id);
      showSuccessToast('Đã tạo lại mã QR cho bàn');
      setFormModal((prev) => ({ ...prev, data: updatedTable }));
      return updatedTable;
    } catch (e) {
      showErrorToast(e);
      return null;
    } finally {
      setIsRegeneratingQr(false);
    }
  };

  const handleSubmitItems = async (payload) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      await orderService.addItemsToOrder(payload);
      showSuccessToast('Đã thêm món vào bàn');
      setAddItemModal({ open: false, table: null });
      queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
    } catch (e) {
      showErrorToast(e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleMergeTables = async (payload) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      await mergeMutation.mutateAsync(payload);
      showSuccessToast('Đã gộp bàn thành công');
      setMergeModal({ open: false, table: null });
      queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
    } catch (e) {
      showErrorToast(e);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen min-w-0 bg-slate-50 p-0 sm:p-3 lg:p-6 dark:bg-slate-950">
      <ManagementHeader
        title="Quản lý bàn ăn"
        showFilter={true}
        filterOptions={tableStatusFilterOptions}
        filterValue={filter}
        setFilterValue={setFilter}
        onAddClick={() => {
          if (checkPermission()) {
            setFormErrors({});
            setFormModal({ open: true, data: null });
          }
        }}
        addButtonText="Thêm bàn"
        hideAddButton={!canManageTables}
      />
      <div className="mt-6">
        {loading ? (
          <div className="flex justify-center p-20">
            <Loader2 className="animate-spin text-orange-500" size={40} />
          </div>
        ) : (
          <TableGrid
            tables={tables.filter((t) => filter === 'ALL' || t.status === filter)}
            orders={orders}
            onAction={handleAction}
            userRole={userRole}
          />
        )}
      </div>

      {/* Modals */}
      <TableFormModal
        key={formModal.open ? formModal.data?.id || 'new' : 'form-closed'}
        isOpen={formModal.open}
        onClose={() => {
          setFormErrors({});
          setFormModal({ open: false, data: null });
        }}
        initialData={formModal.data}
        onSubmit={handleSaveTable}
        isSubmitting={isSubmitting}
        serverErrors={formErrors}
        onClearServerError={(field) => setFormErrors((current) => ({ ...current, [field]: null }))}
        onRegenerateQr={handleRegenerateQr}
        isRegeneratingQr={isRegeneratingQr}
      />
      <AddItemModal
        key={addItemModal.open ? addItemModal.table?.id || 'new' : 'add-item-closed'}
        isOpen={addItemModal.open}
        onClose={() => setAddItemModal({ open: false, table: null })}
        table={addItemModal.table}
        onSubmit={handleSubmitItems}
        isSubmitting={isSubmitting}
      />

      <OrderDetailModal
        key={
          detailModal.open
            ? detailModal.order?.id || detailModal.table?.id || 'new'
            : 'detail-closed'
        }
        isOpen={detailModal.open}
        onClose={() => setDetailModal({ open: false, table: null, order: null })}
        table={detailModal.table}
        order={detailModal.order}
        onOrderUpdate={async ({ reloadOrder = true } = {}) => {
          if (reloadOrder && detailModal.table?.id) {
            const res = await orderService.getCurrentOrder(detailModal.table.id, { force: true });
            setDetailModal((prev) => ({ ...prev, order: res }));
          }
          await queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
        }}
      />

      <PaymentModal
        key={payModal.open ? payModal.order?.id || payModal.table?.id || 'new' : 'pay-closed'}
        isOpen={payModal.open}
        onClose={() => setPayModal({ open: false, table: null, order: null })}
        table={payModal.table}
        order={payModal.order}
        currentUser={user}
        onPaymentSuccess={() => {
          showSuccessToast('Thanh toán thành công');
          const paidOrderId = payModal.order?.id;
          const paidTableIds = new Set(
            [
              payModal.table?.id,
              payModal.table?.primaryTableId,
              ...(payModal.table?.memberTableIds || []),
            ]
              .filter((id) => id != null)
              .map(String),
          );
          queryClient.setQueryData(queryKeys.tables.board, (current) => {
            if (!current) return current;
            return {
              ...current,
              activeOrders: Array.isArray(current.activeOrders)
                ? current.activeOrders.filter((ord) => String(ord.id) !== String(paidOrderId))
                : current.activeOrders,
              tables: Array.isArray(current.tables)
                ? current.tables.map((currentTable) =>
                    paidTableIds.has(String(currentTable.id))
                      ? { ...currentTable, status: TABLE_STATUSES.AVAILABLE }
                      : currentTable,
                  )
                : current.tables,
            };
          });
        }}
      />

      <MergeTableModal
        key={mergeModal.open ? mergeModal.table?.id || 'new' : 'merge-closed'}
        isOpen={mergeModal.open}
        onClose={() => setMergeModal({ open: false, table: null })}
        tables={tables}
        initialTable={mergeModal.table}
        onSubmit={handleMergeTables}
        isSubmitting={isSubmitting}
      />
    </div>
  );
};

export default TableManager;
