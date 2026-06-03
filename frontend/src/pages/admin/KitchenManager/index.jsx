import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, RotateCw } from 'lucide-react';
import { useWebSocket } from '../../../hooks/useWebSocket';
import { orderService } from '../../../services/admin/orderService';
import ManagementHeader from '../../../components/admin/common/ManagementHeader';
import { useStatusModal } from '../../../hooks/useStatusModal';
import StatusModal from '../../../components/admin/common/StatusModal';
import KitchenColumn from './KitchenColumn';
import { playNotificationSound, playLoudSound } from '../../../utils/notificationSound';
import { showBrowserNotification } from '../../../utils/browserNotification';

const KitchenManager = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [processingItems, setProcessingItems] = useState(new Set());
    const { statusModal, showError, closeStatusModal } = useStatusModal();

    const fetchKitchenOrders = useCallback(async () => {
        try {
            const data = await orderService.getKitchenOrders();
            setOrders(data);
        } catch (error) {
            console.error("Failed to fetch kitchen orders:", error);
            showError("Không thể tải danh sách đơn hàng nhà bếp");
        } finally {
            setLoading(false);
        }
    }, [showError]);

    useEffect(() => {
        fetchKitchenOrders();
    }, [fetchKitchenOrders]);

    // Lắng nghe WebSocket từ topic nhà bếp + phát âm thanh
    useWebSocket('/topic/kitchen', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            playLoudSound();
            showBrowserNotification('Đơn hàng nhà bếp', { body: 'Có cập nhật mới cho nhà bếp!' });
            fetchKitchenOrders();
        }
    });

    // Optimistic UI: update local state immediately, then sync with server
    const handleUpdateStatus = async (itemId, newStatus) => {
        if (processingItems.has(itemId)) return;
        setProcessingItems(prev => new Set(prev).add(itemId));

        // Optimistic: update local state
        setOrders(prevOrders =>
            prevOrders.map(order => ({
                ...order,
                orderItems: order.orderItems.map(item =>
                    item.id === itemId ? { ...item, status: newStatus } : item
                ),
            }))
        );

        try {
            await orderService.updateItemStatus(itemId, newStatus);
            // WebSocket sẽ tự trigger reload
        } catch {
            // Rollback on failure
            showError("Không thể cập nhật trạng thái món");
            fetchKitchenOrders();
        } finally {
            setProcessingItems(prev => {
                const next = new Set(prev);
                next.delete(itemId);
                return next;
            });
        }
    };

    const getItemsByStatus = (status) => {
        const items = [];
        orders.forEach(order => {
            order.orderItems.forEach(item => {
                if (item.status === status) {
                    items.push({
                        ...item,
                        tableName: order.table?.tableNumber || 'NaN',
                        orderId: order.id,
                        createdAt: order.createdAt
                    });
                }
            });
        });
        return items.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    };

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader
                title="Nhà bếp"
                showFilter={false}
                onAddClick={fetchKitchenOrders}
                addButtonText="Làm mới"
                addButtonIcon={RotateCw}
            />

            {loading ? (
                <div className="flex justify-center items-center h-64">
                    <Loader2 className="animate-spin text-orange-500" size={40} />
                </div>
            ) : (
                <div className="flex gap-6 mt-6 overflow-x-auto pb-4">
                    <KitchenColumn
                        title="Chờ nấu"
                        status="PENDING"
                        items={getItemsByStatus('PENDING')}
                        accentColor="border-orange-500"
                        onUpdateStatus={handleUpdateStatus}
                        processingItems={processingItems}
                    />
                    <KitchenColumn
                        title="Đang nấu"
                        status="COOKING"
                        items={getItemsByStatus('COOKING')}
                        accentColor="border-indigo-500"
                        onUpdateStatus={handleUpdateStatus}
                        processingItems={processingItems}
                    />
                    <KitchenColumn
                        title="Đã xong"
                        status="FINISHED"
                        items={getItemsByStatus('FINISHED')}
                        accentColor="border-green-500"
                        onUpdateStatus={handleUpdateStatus}
                        processingItems={processingItems}
                    />
                </div>
            )}

            <StatusModal
                isOpen={statusModal.isOpen}
                onClose={closeStatusModal}
                type={statusModal.type}
                title={statusModal.title}
                message={statusModal.message}
            />
        </div>
    );
};

export default KitchenManager;
