import React, { useState, useEffect, useCallback } from 'react';
import { useWebSocket } from '../../../hooks/useWebSocket';
import { useStatusModal } from '../../../hooks/useStatusModal';
import { useAuth } from '../../../context/AuthContext';

import { tableService } from '../../../services/admin/tableService';
import { orderService } from '../../../services/admin/orderService';

import ManagementHeader from '../../../components/admin/common/ManagementHeader';
import TableGrid from './TableGrid';
import TableFormModal from './TableFormModal';
import OrderDetailModal from './OrderDetailModal';
import AddItemModal from './AddItemModal';
import PaymentModal from './PaymentModal';
import StatusModal from '../../../components/admin/common/StatusModal';

const TableManager = () => {
    const [tables, setTables] = useState([]);
    const [orders, setOrders] = useState({});
    const [filter, setFilter] = useState('ALL');

    // Modal States
    const [formModal, setFormModal] = useState({ open: false, data: null });
    const [detailModal, setDetailModal] = useState({ open: false, table: null, order: null });
    const [addItemModal, setAddItemModal] = useState({ open: false, table: null });
    const [payModal, setPayModal] = useState({ open: false, table: null, order: null });
    const [isSubmitting, setIsSubmitting] = useState(false);

    const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

    const { user } = useAuth();
    const userRole = user?.role || localStorage.getItem('role') || 'STAFF';

    const checkPermission = () => {
        if (userRole !== 'MANAGER') {
            showError("Bạn không có quyền thực hiện chức năng này!");
            return false;
        }
        return true;
    };

    // === 1. Fetch Data (ĐÃ TỐI ƯU TỐC ĐỘ & FIX LỖI TIỀN ẢO) ===
    const fetchTables = useCallback(async () => {
        try {
            // 1. Fetch tables and active orders in parallel to reduce sequential blocking
            const [tableData, activeOrders] = await Promise.all([
                tableService.getAll(),
                orderService.getActiveOrders().catch(() => []) // Fallback to empty if fails
            ]);

            const sortedTables = tableData.sort((a, b) => parseInt(a.tableNumber) - parseInt(b.tableNumber));
            setTables(sortedTables);

            // 2. Create map orders from batch results (No more loop-fetching)
            const newOrdersMap = {};
            activeOrders.forEach(ord => {
                if (ord.table && ord.table.id) {
                    newOrdersMap[ord.table.id] = ord;
                }
            });

            setOrders(newOrdersMap);

        } catch (e) { console.error("Error fetching tables/orders:", e); }
    }, []);

    useEffect(() => { fetchTables(); }, [fetchTables]);

    // === 2. Realtime Updates ===
    useWebSocket('/topic/tables', (message) => {
        const signal = typeof message === 'string' ? message : message.body;
        if (signal === 'UPDATED') {
            // console.log("[Realtime] Data changed -> Reloading...");
            fetchTables();
        }
    });

    // 3. Handlers
    const handleAction = ({ type, table, order }) => {
        switch (type) {
            case 'ADD_ITEM': setAddItemModal({ open: true, table }); break;
            case 'DETAIL':
                // Luôn fetch lại order mới nhất khi mở modal
                orderService.getCurrentOrder(table.id)
                    .then(res => setDetailModal({ open: true, table, order: res || null }))
                    .catch(() => setDetailModal({ open: true, table, order: null }));
                break;
            case 'PAY': setPayModal({ open: true, table, order }); break;

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
            // Không cần gọi fetchTables vì Socket sẽ tự gọi
        } catch (e) { showError(e); }
        finally { setIsSubmitting(false); }
    };

    const handleDeleteTable = async (id) => {
        if (!confirm("Xóa bàn này?")) return;
        try {
            await tableService.delete(id);
            showSuccess("Đã xóa bàn");
            // Không cần gọi fetchTables vì Socket sẽ tự gọi
        } catch (e) { showError(e); }
    };

    const handleSubmitItems = async (payload) => {
        if (isSubmitting) return;
        setIsSubmitting(true);
        try {
            await orderService.addItemsToOrder(payload);
            showSuccess("Đã thêm món vào bàn");
            setAddItemModal({ open: false, table: null });
        } catch (e) { showError(e); }
        finally { setIsSubmitting(false); }
    };

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader
                title="Quản lý bàn ăn"
                showFilter={true}
                filterOptions={[
                    { id: 'AVAILABLE', name: 'Trống' },
                    { id: 'OCCUPIED', name: 'Đang phục vụ' },
                    { id: 'WAITING_FOR_PAYMENT', name: 'Chờ thanh toán' }
                ]}
                filterValue={filter}
                setFilterValue={setFilter}

                onAddClick={() => {
                    if (checkPermission()) setFormModal({ open: true, data: null });
                }}
                addButtonText="Thêm bàn"
            />

            <div className="mt-6">
                <TableGrid
                    tables={tables.filter(t => filter === 'ALL' || t.status === filter)}
                    orders={orders}
                    onAction={handleAction}
                    userRole={userRole}
                />
            </div>

            {/* Modals */}
            <TableFormModal
                key={formModal.open ? (formModal.data?.id || 'new') : 'form-closed'}
                isOpen={formModal.open} onClose={() => setFormModal({ open: false, data: null })} initialData={formModal.data} onSubmit={handleSaveTable} isSubmitting={isSubmitting} />
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
                }}
            />

            <PaymentModal
                key={payModal.open ? (payModal.order?.id || payModal.table?.id || 'new') : 'pay-closed'}
                isOpen={payModal.open} onClose={() => setPayModal({ open: false, table: null, order: null })}
                table={payModal.table} order={payModal.order}
                onPaymentSuccess={() => { showSuccess("Thanh toán thành công"); }}
            />

            <StatusModal isOpen={statusModal.isOpen} onClose={closeStatusModal} type={statusModal.type} title={statusModal.title} message={statusModal.message} />
        </div>
    );
};

export default TableManager;