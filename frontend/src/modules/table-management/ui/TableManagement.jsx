import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2 } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useAuth } from '@modules/auth/model/AuthContext.jsx';

import { tableService } from '@modules/table-management/api/tableService.js';
import { orderService } from '@modules/order-management/api/orderService.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import TableFormModal from './TableFormModal.jsx';
import TableGrid from './TableGrid.jsx';
import OrderDetailModal from './OrderDetailModal.jsx';
import AddItemModal from './AddItemModal.jsx';
import PaymentModal from './PaymentModal.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';
import { TABLE_STATUS } from '@entities/order/lib/orderStatus.js';

const tableStatusFilterOptions = Object.entries(TABLE_STATUS).map(([id, meta]) => ({
    id,
    name: meta.label
}));

const TableManager = () => {
    const [tables, setTables] = useState([]);
    const [orders, setOrders] = useState({});
    const [filter, setFilter] = useState('ALL');
    const [loading, setLoading] = useState(false);

    // Modal States
    const [formModal, setFormModal] = useState({ open: false, data: null });
    const [detailModal, setDetailModal] = useState({ open: false, table: null, order: null });
    const [addItemModal, setAddItemModal] = useState({ open: false, table: null });
    const [payModal, setPayModal] = useState({ open: false, table: null, order: null });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isRegeneratingQr, setIsRegeneratingQr] = useState(false);
    const isMountedRef = useRef(true);
    const fetchSeqRef = useRef(0);

    const { showSuccess, showError } = useStatusModal();
    const { confirm } = useConfirmModal();

    const { user } = useAuth();
    const userRole = user?.role || 'STAFF';

    const checkPermission = () => {
        if (userRole !== 'MANAGER') {
            showError("Bạn không có quyền thực hiện chức năng này!");
            return false;
        }
        return true;
    };

    useEffect(() => {
        isMountedRef.current = true;
        return () => {
            isMountedRef.current = false;
        };
    }, []);

    const fetchTables = useCallback(async (showLoading = false) => {
        const fetchSeq = ++fetchSeqRef.current;
        if (showLoading) setLoading(true);
        try {
            const board = await orderService.getTableBoard();

            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;

            const tableData = Array.isArray(board?.tables) ? board.tables : [];
            const activeOrders = Array.isArray(board?.activeOrders) ? board.activeOrders : [];
            const sortedTables = [...tableData].sort((a, b) =>
                String(a.tableNumber).localeCompare(String(b.tableNumber), undefined, { numeric: true })
            );
            setTables(sortedTables);

            const newOrdersMap = {};
            activeOrders.forEach(ord => {
                if (ord.tableId && !newOrdersMap[ord.tableId]) {
                    newOrdersMap[ord.tableId] = {
                        id: ord.id,
                        status: ord.status,
                        finalAmount: ord.finalAmount,
                        createdAt: ord.createdAt,
                        table: {
                            id: ord.tableId,
                            tableNumber: ord.tableNumber
                        }
                    };
                }
            });

            setOrders(newOrdersMap);

        } catch (e) {
            if (isMountedRef.current) console.error("Error fetching table board:", e);
        } finally {
            if (isMountedRef.current && fetchSeq === fetchSeqRef.current) setLoading(false);
        }
    }, []);

    useEffect(() => { fetchTables(true); }, [fetchTables]);

    // === 2. Realtime Updates ===
    useWebSocket('/topic/tables', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            fetchTables();
        }
    });

    // 3. Handlers
    const handleAction = async ({ type, table }) => {
        switch (type) {
            case 'ADD_ITEM': setAddItemModal({ open: true, table }); break;
            case 'DETAIL':
                // Luôn fetch lại order mới nhất khi mở modal
                orderService.getCurrentOrder(table.id)
                    .then(res => setDetailModal({ open: true, table, order: res || null }))
                    .catch(() => setDetailModal({ open: true, table, order: null }));
                break;
            case 'PAY':
                try {
                    const latestOrder = await orderService.getCurrentOrder(table.id);
                    if (!latestOrder) {
                        showError("Không tìm thấy đơn đang hoạt động của bàn này");
                        fetchTables();
                        break;
                    }
                    setPayModal({ open: true, table, order: latestOrder });
                } catch (error) {
                    showError(error);
                }
                break;

            case 'EDIT':
                if (checkPermission()) setFormModal({ open: true, data: table });
                break;

            case 'DELETE':
                if (checkPermission()) handleDeleteTable(table.id);
                break;
            default: break;
        }
    };

    const handleSaveTable = async (data) => {
        if (isSubmitting) return;
        setIsSubmitting(true);
        try {
            if (data.id) await tableService.update(data.id, data);
            else await tableService.create(data);
            showSuccess(data.id ? "Cập nhật bàn thành công" : "Thêm bàn thành công");
            setFormModal({ open: false, data: null });
            fetchTables(); // Refresh immediately
        } catch (e) {
            const errorMsg = e.message || '';
            if (errorMsg.includes("Table number already exists")) {
                showError("Số bàn này đã tồn tại");
            } else {
                showError(e);
            }
        }
        finally { setIsSubmitting(false); }
    };

    const handleDeleteTable = async (id) => {
        const confirmed = await confirm('Xóa bàn', 'Bạn có chắc chắn muốn xóa bàn này?');
        if (!confirmed) return;
        try {
            await tableService.delete(id);
            showSuccess("Đã xóa bàn");
            fetchTables(); // Refresh immediately
        } catch (e) { showError(e); }
    };

    const handleRegenerateQr = async (id) => {
        if (isRegeneratingQr) return null;
        const confirmed = await confirm(
            'Tạo lại mã QR',
            'Mã QR cũ của bàn này sẽ không còn dùng được. Bạn có chắc chắn muốn xóa QR cũ và tạo mã QR mới?'
        );
        if (!confirmed) return null;

        setIsRegeneratingQr(true);
        try {
            const updatedTable = await tableService.regenerateQr(id);
            showSuccess("Đã tạo lại mã QR cho bàn");
            setFormModal(prev => ({ ...prev, data: updatedTable }));
            fetchTables();
            return updatedTable;
        } catch (e) {
            showError(e);
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
            showSuccess("Đã thêm món vào bàn");
            setAddItemModal({ open: false, table: null });
            fetchTables(); // Refresh immediately
        } catch (e) { showError(e); }
        finally { setIsSubmitting(false); }
    };

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader
                title="Quản lý bàn ăn"
                showFilter={true}
                filterOptions={tableStatusFilterOptions}
                filterValue={filter}
                setFilterValue={setFilter}

                onAddClick={() => {
                    if (checkPermission()) setFormModal({ open: true, data: null });
                }}
                addButtonText="Thêm bàn"
            />
            <div className="mt-6">
                {loading ? (
                    <div className="flex justify-center p-20">
                        <Loader2 className="animate-spin text-orange-500" size={40} />
                    </div>
                ) : (
                    <TableGrid
                        tables={tables.filter(t => filter === 'ALL' || t.status === filter)}
                        orders={orders}
                        onAction={handleAction}
                        userRole={userRole}
                    />
                )}
            </div>

            {/* Modals */}
            <TableFormModal
                key={formModal.open ? (formModal.data?.id || 'new') : 'form-closed'}
                isOpen={formModal.open} onClose={() => setFormModal({ open: false, data: null })} initialData={formModal.data} onSubmit={handleSaveTable} isSubmitting={isSubmitting}
                onRegenerateQr={handleRegenerateQr} isRegeneratingQr={isRegeneratingQr} />
            <AddItemModal
                key={addItemModal.open ? (addItemModal.table?.id || 'new') : 'add-item-closed'}
                isOpen={addItemModal.open} onClose={() => setAddItemModal({ open: false, table: null })} table={addItemModal.table} onSubmit={handleSubmitItems} isSubmitting={isSubmitting} />

            <OrderDetailModal
                key={detailModal.open ? (detailModal.order?.id || detailModal.table?.id || 'new') : 'detail-closed'}
                isOpen={detailModal.open} onClose={() => setDetailModal({ open: false, table: null, order: null })}
                table={detailModal.table} order={detailModal.order}
                onOrderUpdate={async () => {
                    // Khi cập nhật món (bếp xong/hủy), cần reload lại order ngay trong modal
                    const res = await orderService.getCurrentOrder(detailModal.table.id);
                    setDetailModal(prev => ({ ...prev, order: res }));
                    await fetchTables();
                }}
            />

            <PaymentModal
                key={payModal.open ? (payModal.order?.id || payModal.table?.id || 'new') : 'pay-closed'}
                isOpen={payModal.open} onClose={() => setPayModal({ open: false, table: null, order: null })}
                table={payModal.table} order={payModal.order}
                currentUser={user}
                onPaymentSuccess={() => {
                    showSuccess("Thanh toán thành công");
                    fetchTables();
                }}
            />
        </div>
    );
};

export default TableManager;
