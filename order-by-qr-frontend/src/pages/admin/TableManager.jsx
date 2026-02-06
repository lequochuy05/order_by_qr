import React, { useState, useEffect, useCallback } from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useStatusModal } from '../../hooks/useStatusModal';
import { useAuth } from '../../context/AuthContext'; 

import { tableService } from '../../services/admin/tableService';
import { orderService } from '../../services/admin/orderService';

import ManagementHeader from '../../components/admin/common/ManagementHeader';
import TableGrid from '../../components/admin/table/TableGrid';
import TableFormModal from '../../components/admin/table/TableFormModal';
import OrderDetailModal from '../../components/admin/table/OrderDetailModal';
import AddItemModal from '../../components/admin/table/AddItemModal';
import PaymentModal from '../../components/admin/table/PaymentModal';
import StatusModal from '../../components/admin/common/StatusModal';

const TableManager = () => {
    const [tables, setTables] = useState([]);
    const [orders, setOrders] = useState({}); 
    const [filter, setFilter] = useState('ALL');
    
    // Modal States
    const [formModal, setFormModal] = useState({ open: false, data: null });
    const [detailModal, setDetailModal] = useState({ open: false, table: null, order: null });
    const [addItemModal, setAddItemModal] = useState({ open: false, table: null });
    const [payModal, setPayModal] = useState({ open: false, table: null, order: null });

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
            // 1. Lấy danh sách bàn
            const tableData = await tableService.getAll();
            const sortedTables = tableData.sort((a,b) => parseInt(a.tableNumber) - parseInt(b.tableNumber));
            setTables(sortedTables);
            
            // 2. Lấy danh sách đơn hàng song song (Promise.all) để tăng tốc độ
            const activeTables = sortedTables.filter(t => t.status !== 'Trống');
            
            const orderPromises = activeTables.map(t => 
                orderService.getCurrentOrder(t.id)
                    .then(ord => ({ tableId: t.id, order: ord }))
                    .catch(() => ({ tableId: t.id, order: null }))
            );

            const results = await Promise.all(orderPromises);

            // 3. Tạo map orders mới hoàn toàn (Xóa sạch dữ liệu cũ để tránh lỗi hiển thị tiền bàn trống)
            const newOrdersMap = {};
            results.forEach(res => {
                if (res.order) {
                    newOrdersMap[res.tableId] = res.order;
                }
            });
            
            // Cập nhật 1 lần duy nhất (giảm số lần render -> mượt hơn)
            setOrders(newOrdersMap);

        } catch (e) { console.error(e); }
    }, []);

    useEffect(() => { fetchTables(); }, [fetchTables]);

    // === 2. Realtime Updates ===
    useWebSocket('/topic/tables', (message) => {
        const signal = typeof message === 'string' ? message : message.body;
        if (signal === 'UPDATED') {
            console.log("[Realtime] Data changed -> Reloading...");
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
        try {
            if (data.id) await tableService.update(data.id, data);
            else await tableService.create(data);
            showSuccess(data.id ? "Cập nhật bàn thành công" : "Thêm bàn thành công");
            setFormModal({ open: false, data: null });
            // Không cần gọi fetchTables vì Socket sẽ tự gọi
        } catch (e) { showError(e); }
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
        try {
            await orderService.addItemsToOrder(payload);
            showSuccess("Đã thêm món vào bàn");
            setAddItemModal({ open: false, table: null });
        } catch (e) { showError(e); }
    };

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader 
                title="Quản lý bàn ăn"
                showFilter={true}
                filterOptions={[{id: 'Trống', name: 'Trống'}, {id: 'Đang phục vụ', name: 'Đang phục vụ'},{id: 'Chờ thanh toán', name: 'Chờ thanh toán'}]}
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
            <TableFormModal isOpen={formModal.open} onClose={() => setFormModal({ open: false, data: null })} initialData={formModal.data} onSubmit={handleSaveTable} />
            <AddItemModal isOpen={addItemModal.open} onClose={() => setAddItemModal({ open: false, table: null })} table={addItemModal.table} onSubmit={handleSubmitItems} />
            
            <OrderDetailModal 
                isOpen={detailModal.open} onClose={() => setDetailModal({ open: false, table: null, order: null })}
                table={detailModal.table} order={detailModal.order}
                onOrderUpdate={async () => {
                    // Khi cập nhật món (bếp xong/hủy), cần reload lại order ngay trong modal
                    const res = await orderService.getCurrentOrder(detailModal.table.id);
                    setDetailModal(prev => ({ ...prev, order: res }));
                }}
            />

            <PaymentModal 
                isOpen={payModal.open} onClose={() => setPayModal({ open: false, table: null, order: null })}
                table={payModal.table} order={payModal.order}
                onPaymentSuccess={() => { showSuccess("Thanh toán thành công"); }}
            />

            <StatusModal isOpen={statusModal.isOpen} onClose={closeStatusModal} type={statusModal.type} title={statusModal.title} message={statusModal.message} />
        </div>
    );
};

export default TableManager;