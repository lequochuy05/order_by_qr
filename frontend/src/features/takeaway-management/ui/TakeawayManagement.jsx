import React, { useState } from 'react';
import { ShoppingBag, Plus, Loader2, Clock, AlertCircle, RefreshCw } from 'lucide-react';
import { useAuth } from '@features/auth';
import { orderService } from '@entities/order/api/orderService.js';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import { getOrderStatusMeta, ORDER_STATUSES, ORDER_TYPES } from '@entities/order/lib/orderStatus.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import AddItemModal from '@features/table-management/ui/AddItemModal.jsx';
import OrderDetailModal from '@features/table-management/ui/OrderDetailModal.jsx';
import PaymentModal from '@features/table-management/ui/PaymentModal.jsx';
import { useTakeawayOrders } from '../model/useTakeawayOrders.js';

const TakeawayManagement = () => {
  const { user } = useAuth();
  const [addItemModal, setAddItemModal] = useState({ open: false });
  const [detailModal, setDetailModal] = useState({ open: false, order: null });
  const [payModal, setPayModal] = useState({ open: false, order: null });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { data: orders = [], isLoading, error, refetch } = useTakeawayOrders();

  const handleCreateOrder = () => {
    setAddItemModal({ open: true });
  };

  const handleSubmitItems = async (payload) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      await orderService.addItemsToOrder({
        ...payload,
        orderType: ORDER_TYPES.TAKEAWAY,
      });
      showSuccessToast('Đã tạo đơn mang về');
      setAddItemModal({ open: false });
      queryClient.invalidateQueries({ queryKey: queryKeys.orders.activeTakeaway });
    } catch (e) {
      showErrorToast(e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDetail = async (order) => {
    const fullOrder = order?.id ? await orderService.getOrderById(order.id) : null;
    setDetailModal({ open: true, order: fullOrder || order });
  };

  const handlePay = async (order) => {
    try {
      const updated = await orderService.updateOrderStatus(order.id, ORDER_STATUSES.AWAITING_PAYMENT);
      setPayModal({ open: true, order: updated || order });
    } catch (e) {
      showErrorToast(e);
    }
  };

  return (
    <div className="min-h-screen min-w-0 bg-transparent p-0 sm:p-3 lg:p-6">
      <ManagementHeader
        title="Bán mang về"
        addButtonText="Tạo đơn mới"
        onAddClick={handleCreateOrder}
        hideAddButton={false}
      />

      <div className="mt-6">
        {isLoading ? (
          <div className="flex justify-center p-20">
            <Loader2 className="animate-spin text-[#C17C4A]" size={40} />
          </div>
        ) : error ? (
          <div className="flex min-h-[420px] items-center justify-center p-6">
            <div className="rounded-3xl border border-rose-200 dark:border-rose-900/60 bg-rose-50 dark:bg-rose-950/30 p-8 text-center max-w-sm shadow-sm">
              <AlertCircle className="mx-auto mb-3 text-rose-500" size={32} />
              <p className="font-bold text-rose-700 dark:text-rose-400 mb-1">Không thể tải dữ liệu</p>
              <button
                onClick={() => refetch()}
                className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 text-sm font-bold text-white hover:bg-rose-700 transition-colors mt-4 shadow-sm"
              >
                <RefreshCw size={14} />
                Thử lại
              </button>
            </div>
          </div>
        ) : orders.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-[#9C8E85] dark:text-[#82756B] space-y-4">
            <div className="w-24 h-24 bg-[#F7F0E7] dark:bg-[#2A221E] rounded-3xl flex items-center justify-center shadow-sm">
              <ShoppingBag size={40} className="text-[#C17C4A]" />
            </div>
            <div className="text-center">
              <p className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">Chưa có đơn mang về</p>
              <p className="text-sm font-medium text-[#6B5D55] dark:text-[#BDB0A4] mt-1">Bấm "Tạo đơn mới" để bắt đầu phục vụ khách mang đi.</p>
            </div>
          </div>
        ) : (
          <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {orders.map((order) => {
              const finalAmount = getOrderFinalAmount(order);
              const statusMeta = getOrderStatusMeta(order.status);
              const itemCount = order.orderItems?.length || 0;
              const timeAgo = order.createdAt
                ? (() => {
                    const diff = Date.now() - new Date(order.createdAt).getTime();
                    const mins = Math.floor(diff / 60000);
                    return mins < 60 ? `${mins} phút` : `${Math.floor(mins / 60)}h ${mins % 60}p`;
                  })()
                : '';

              return (
                <div
                  key={order.id}
                  className="bg-white dark:bg-[#1F1A17] rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] p-5 shadow-sm hover:shadow-md transition-all"
                >
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div>
                      <h3 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">
                        Đơn #{order.id}
                      </h3>
                      <div className="flex items-center gap-2 text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4] mt-1">
                        <Clock size={12} />
                        <span>{timeAgo}</span>
                      </div>
                    </div>
                    <span
                      className={`shrink-0 whitespace-nowrap rounded-full px-3 py-1 text-[10px] font-bold uppercase ${statusMeta.classes}`}
                    >
                      {statusMeta.label}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5 text-sm font-semibold text-[#6B5D55] dark:text-[#BDB0A4] mb-3">
                    <ShoppingBag size={14} className="text-[#C17C4A]" />
                    <span>{itemCount} món</span>
                  </div>

                  <div className="mb-4">
                    <p className="text-xs font-bold uppercase tracking-wider text-[#9C8E85] dark:text-[#82756B]">Tổng tiền:</p>
                    <p className="text-2xl font-black text-[#56372B] dark:text-[#D8986A]">
                      {fmtVND(finalAmount)}
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => handleDetail(order)}
                      className="flex items-center justify-center gap-1 rounded-xl border border-[#E8DCD1] dark:border-[#352C26] bg-white dark:bg-[#1F1A17] px-3 py-2.5 text-xs font-bold text-[#56372B] dark:text-[#F7F0E7] hover:bg-[#F7F0E7] dark:hover:bg-[#2A221E] transition-colors"
                    >
                      Chi tiết
                    </button>
                    {finalAmount > 0 && (
                      <button
                        onClick={() => handlePay(order)}
                        className="flex items-center justify-center gap-1 rounded-xl bg-emerald-600 px-3 py-2.5 text-xs font-black uppercase tracking-wider text-white shadow-md shadow-emerald-600/20 hover:bg-emerald-700 transition-colors"
                      >
                        Thanh toán
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Modals - reuse from table-management */}
      <AddItemModal
        key={addItemModal.open ? 'takeaway-add' : 'add-closed'}
        isOpen={addItemModal.open}
        onClose={() => setAddItemModal({ open: false })}
        table={null}
        onSubmit={handleSubmitItems}
        isSubmitting={isSubmitting}
      />

      <OrderDetailModal
        key={detailModal.open ? detailModal.order?.id || 'detail-closed' : 'detail-closed'}
        isOpen={detailModal.open}
        onClose={() => setDetailModal({ open: false, order: null })}
        table={{}}
        order={detailModal.order}
        onOrderUpdate={async () => {
          if (detailModal.order?.id) {
            const res = await orderService.getOrderById(detailModal.order.id);
            setDetailModal((prev) => ({ ...prev, order: res }));
            await queryClient.invalidateQueries({ queryKey: queryKeys.orders.activeTakeaway });
          }
        }}
      />

      <PaymentModal
        key={payModal.open ? payModal.order?.id || 'pay-closed' : 'pay-closed'}
        isOpen={payModal.open}
        onClose={() => setPayModal({ open: false, order: null })}
        table={{}}
        order={payModal.order}
        currentUser={user}
        onPaymentSuccess={() => {
          showSuccessToast('Thanh toán thành công');
          queryClient.invalidateQueries({ queryKey: queryKeys.orders.activeTakeaway });
        }}
      />
    </div>
  );
};

export default TakeawayManagement;
