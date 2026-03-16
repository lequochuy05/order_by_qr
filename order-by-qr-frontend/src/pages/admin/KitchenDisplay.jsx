import React, { useState, useEffect, useCallback } from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { orderService } from '../../services/admin/orderService';
import ManagementHeader from '../../components/admin/common/ManagementHeader';
import { useStatusModal } from '../../hooks/useStatusModal';
import StatusModal from '../../components/admin/common/StatusModal';
import { fmtDateTime } from '../../utils/formatters';

const KitchenDisplay = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
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

    // Lắng nghe WebSocket từ topic nhà bếp
    useWebSocket('/topic/kitchen', (message) => {
        const signal = typeof message === 'string' ? message : message.body;
        if (signal === 'UPDATED') {
            fetchKitchenOrders();
        }
    });

    const handleUpdateStatus = async (itemId, newStatus) => {
        try {
            await orderService.updateItemStatus(itemId, newStatus);
            // WebSocket sẽ tự trigger reload
        } catch (error) {
            showError("Không thể cập nhật trạng thái món");
        }
    };

    const getItemsByStatus = (status) => {
        const items = [];
        orders.forEach(order => {
            order.orderItems.forEach(item => {
                if (item.status === status) {
                    items.push({ 
                        ...item, 
                        tableName: order.table?.tableNumber || 'N/A', 
                        orderId: order.id, 
                        createdAt: order.createdAt 
                    });
                }
            });
        });
        return items.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    };

    const StatusColumn = ({ title, status, items, accentColor }) => (
        <div className="flex-1 min-w-[300px] bg-slate-100 rounded-lg p-4 flex flex-col h-[calc(100vh-200px)]">
            <div className={`flex items-center gap-2 mb-4 pb-2 border-b-2 ${accentColor}`}>
                <h2 className="text-lg font-bold text-slate-700">{title}</h2>
                <span className="bg-slate-200 text-slate-600 px-2 py-0.5 rounded-full text-sm font-medium">
                    {items.length}
                </span>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 pr-2 scrollbar-thin scrollbar-thumb-slate-300">
                {items.length === 0 ? (
                    <div className="text-center py-10 text-slate-400 italic">Trống</div>
                ) : (
                    items.map(item => (
                        <div key={item.id} className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 hover:shadow-md transition-shadow">
                            <div className="flex justify-between items-start mb-2">
                                <span className="bg-orange-100 text-orange-700 px-2 py-1 rounded-md text-sm font-bold">
                                    Bàn: {item.tableName}
                                </span>
                                <span className="text-xs text-slate-400">
                                    {fmtDateTime(item.createdAt).split(' ')[1]}
                                </span>
                            </div>

                            <h3 className="font-semibold text-slate-800 text-lg">
                                {item.menuItem?.name || item.combo?.name || 'Món không tên'}
                                {item.quantity > 1 && <span className="text-orange-600 ml-2">x{item.quantity}</span>}
                            </h3>

                            {item.notes && (
                                <div className="mt-2 p-2 bg-yellow-50 border border-yellow-100 rounded text-sm text-yellow-800 italic">
                                    Ghi chú: {item.notes}
                                </div>
                            )}

                            <div className="mt-4 flex gap-2">
                                {status === 'PENDING' && (
                                    <button
                                        onClick={() => handleUpdateStatus(item.id, 'COOKING')}
                                        className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg text-sm font-medium transition-colors"
                                    >
                                        Bắt đầu nấu
                                    </button>
                                )}
                                {status === 'COOKING' && (
                                    <button
                                        onClick={() => handleUpdateStatus(item.id, 'FINISHED')}
                                        className="flex-1 bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg text-sm font-medium transition-colors"
                                    >
                                        Hoàn thành
                                    </button>
                                )}
                                {status === 'FINISHED' && (
                                    <button
                                        onClick={() => handleUpdateStatus(item.id, 'COOKING')}
                                        className="text-slate-400 hover:text-slate-600 text-xs flex items-center gap-1 mx-auto mt-1"
                                    >
                                        ↩ Hoàn tác
                                    </button>
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );

    return (
        <div className="p-6 bg-slate-50 min-h-screen">
            <ManagementHeader
                title="Màn hình Nhà bếp (KDS)"
                showFilter={false}
                onAddClick={fetchKitchenOrders}
                addButtonText="Làm mới"
            />

            {loading ? (
                <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                </div>
            ) : (
                <div className="flex gap-6 mt-6 overflow-x-auto pb-4">
                    <StatusColumn
                        title="Chờ nấu"
                        status="PENDING"
                        items={getItemsByStatus('PENDING')}
                        accentColor="border-orange-500"
                    />
                    <StatusColumn
                        title="Đang nấu"
                        status="COOKING"
                        items={getItemsByStatus('COOKING')}
                        accentColor="border-indigo-500"
                    />
                    <StatusColumn
                        title="Đã xong"
                        status="FINISHED"
                        items={getItemsByStatus('FINISHED')}
                        accentColor="border-green-500"
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

export default KitchenDisplay;
