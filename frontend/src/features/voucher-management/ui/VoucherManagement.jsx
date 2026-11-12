import React, { useState, useEffect, useRef } from 'react';
import { Loader2, Ticket } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import {
  useCreateVoucherMutation,
  useDeleteVoucherMutation,
  useUpdateVoucherMutation,
  useVoucherPageQuery,
} from '@entities/voucher';
import { VOUCHER_STATUSES } from '@entities/voucher/lib/voucherStatus.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import EditDeleteActions from '@shared/ui/EditDeleteActions.jsx';
import VoucherModal from './VoucherModal.jsx';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { fmtVND, fmtDate } from '@shared/lib/formatters.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const VOUCHER_STATUS = {
  [VOUCHER_STATUSES.ACTIVE]: {
    text: 'Đang dùng',
    color: 'bg-green-100 text-green-700 dark:bg-green-500/10 dark:text-green-300',
  },
  [VOUCHER_STATUSES.INACTIVE]: {
    text: 'Ngừng',
    color: 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400',
  },
  [VOUCHER_STATUSES.EXPIRED]: {
    text: 'Hết hạn',
    color: 'bg-red-100 text-red-600 dark:bg-red-500/10 dark:text-red-300',
  },
  [VOUCHER_STATUSES.EXHAUSTED]: {
    text: 'Hết lượt',
    color: 'bg-amber-100 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300',
  },
};

const voucherStatusFilterOptions = Object.entries(VOUCHER_STATUS).map(([id, meta]) => ({
  id,
  name: meta.text,
}));

const VoucherManager = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [formErrors, setFormErrors] = useState({});
  const isMountedRef = useRef(true);
  const pageSize = 24;

  const { confirm } = useConfirmModal();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);
  const queryParams = {
    page: currentPage,
    size: pageSize,
    ...(debouncedSearchTerm.trim() ? { q: debouncedSearchTerm.trim() } : {}),
    ...(statusFilter !== 'ALL' ? { status: statusFilter } : {}),
  };
  const { data: voucherData, isFetching: loading } = useVoucherPageQuery(queryParams);
  const vouchers = voucherData?.content || [];
  const totalPages = voucherData?.totalPages || 0;
  const totalElements = voucherData?.totalElements || 0;
  const { mutateAsync: createVoucher } = useCreateVoucherMutation();
  const { mutateAsync: updateVoucher } = useUpdateVoucherMutation();
  const { mutateAsync: deleteVoucher } = useDeleteVoucherMutation();

  const getStatusInfo = (v) => {
    const now = new Date();
    const from = v.validFrom ? new Date(v.validFrom) : null;
    const to = v.validTo ? new Date(v.validTo) : null;
    const isUpcoming = from && now < from;
    const isExpired = to && now > to;
    const isExhausted = v.usageLimit > 0 && v.usedCount >= v.usageLimit;
    let value = VOUCHER_STATUSES.ACTIVE;

    if (!v.active) value = VOUCHER_STATUSES.INACTIVE;
    else if (isUpcoming) value = VOUCHER_STATUSES.INACTIVE;
    else if (isExpired) value = VOUCHER_STATUSES.EXPIRED;
    else if (isExhausted) value = VOUCHER_STATUSES.EXHAUSTED;

    return { value, ...VOUCHER_STATUS[value] };
  };

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useWebSocket(
    '/topic/vouchers',
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        playNotificationSound();
      }
    },
    { scope: 'admin' },
  );

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, statusFilter]);

  const handleSubmit = async (data) => {
    setIsSaving(true);
    const safeValidFrom =
      data.validFrom && !data.validFrom.includes('T')
        ? `${data.validFrom}T00:00:00`
        : data.validFrom;
    const safeValidTo =
      data.validTo && !data.validTo.includes('T') ? `${data.validTo}T23:59:59` : data.validTo;

    const payload = {
      ...data,
      code: data.code.trim().toUpperCase(),
      type: data.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
      discountPercent: data.discountPercent ? parseFloat(data.discountPercent) : null,
      discountAmount: data.discountAmount ? parseFloat(data.discountAmount) : null,
      usageLimit: parseInt(data.usageLimit) || 0,
      validFrom: safeValidFrom,
      validTo: safeValidTo,
    };

    try {
      if (editingVoucher?.id) {
        await updateVoucher({ id: editingVoucher.id, data: payload });
        showSuccessToast(`Đã cập nhật voucher`);
      } else {
        await createVoucher(payload);
        showSuccessToast(`Đã thêm voucher`);
      }
      setFormErrors({});
      setIsModalOpen(false);
    } catch (err) {
      const errorMsg = err.message || '';
      if (err?.code === 'VOUCHER_CODE_EXISTS' || errorMsg.includes('Voucher code already exists')) {
        setFormErrors({ code: 'Mã voucher này đã tồn tại trên hệ thống' });
      } else {
        showErrorToast(err);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa voucher', 'Bạn chắc chắn muốn xóa voucher này?');
    if (!confirmed) return;
    try {
      await deleteVoucher(id);
      showSuccessToast('Đã xóa voucher thành công');
    } catch (err) {
      showErrorToast(err);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm mã voucher..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={() => {
          setEditingVoucher(null);
          setFormErrors({});
          setIsModalOpen(true);
        }}
        addButtonText="Thêm Voucher"
        showFilter
        filterAllLabel="Tất cả trạng thái"
        filterValue={statusFilter}
        setFilterValue={setStatusFilter}
        filterOptions={voucherStatusFilterOptions}
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-6 md:grid-cols-2 xl:grid-cols-4">
          {vouchers.map((v) => {
            const status = getStatusInfo(v);
            return (
              <div
                key={v.id}
                className="flex h-full flex-col rounded-3xl border border-[#E8DCD1] bg-white p-6 shadow-sm transition-all hover:border-[#C17C4A] hover:shadow-md dark:border-[#352C26] dark:bg-[#1F1A17]"
              >
                <div className="flex justify-between items-start mb-4">
                  <div className="rounded-2xl bg-[#F7F0E7] p-3 text-[#C17C4A] dark:bg-[#2A221E] dark:text-[#D8986A]">
                    <Ticket size={24} />
                  </div>
                  <span
                    className={`text-[10px] font-black px-3 py-1 rounded-full uppercase ${status.color}`}
                  >
                    {status.text}
                  </span>
                </div>

                <h3 className="mb-1 text-xl font-black uppercase tracking-wider text-[#2D1D16] dark:text-[#F7F0E7]">
                  {v.code}
                </h3>
                <div className="mb-4 text-2xl font-black text-[#56372B] dark:text-[#D8986A]">
                  {v.discountPercent ? `${v.discountPercent}%` : fmtVND(v.discountAmount)}
                </div>
                {/* ... Thông tin chi tiết ... */}
                <div className="mb-6 space-y-2 text-xs text-[#6B5D55] dark:text-[#BDB0A4]">
                  <div className="flex justify-between">
                    <span>Đã dùng:</span>{' '}
                    <span className="font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
                      {v.usedCount}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span>Giới hạn:</span>{' '}
                    <span className="font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
                      {v.usageLimit === 0 ? '∞' : v.usageLimit}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span>Hạn dùng:</span>{' '}
                    <span className="font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
                      {fmtDate(v.validTo) || 'Vô hạn'}
                    </span>
                  </div>
                </div>

                <EditDeleteActions
                  onEdit={() => {
                    setEditingVoucher(v);
                    setFormErrors({});
                    setIsModalOpen(true);
                  }}
                  onDelete={() => handleDelete(v.id)}
                />
              </div>
            );
          })}
        </div>
      )}

      {!loading && vouchers.length === 0 && (
        <div className="rounded-3xl border border-dashed border-[#E8DCD1] bg-white py-20 text-center italic text-[#9C8E85] transition-colors dark:border-[#352C26] dark:bg-[#1F1A17] dark:text-[#82756B]">
          <Ticket size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy voucher nào.
        </div>
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="voucher"
        loading={loading}
        onPageChange={setCurrentPage}
      />

      {/* Modal Nhập liệu */}
      <VoucherModal
        key={isModalOpen ? editingVoucher?.id || 'new' : 'closed'}
        isOpen={isModalOpen}
        onClose={() => {
          setFormErrors({});
          setIsModalOpen(false);
        }}
        initialData={editingVoucher}
        onSubmit={handleSubmit}
        isSubmitting={isSaving}
        serverErrors={formErrors}
        onClearServerError={(field) => setFormErrors((current) => ({ ...current, [field]: null }))}
      />
    </div>
  );
};

export default VoucherManager;
