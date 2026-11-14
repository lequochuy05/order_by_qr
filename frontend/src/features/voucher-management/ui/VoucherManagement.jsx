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
    color: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
  },
  [VOUCHER_STATUSES.INACTIVE]: {
    text: 'Ngừng',
    color: 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400',
  },
  [VOUCHER_STATUSES.EXPIRED]: {
    text: 'Hết hạn',
    color: 'bg-rose-100 text-rose-600 dark:bg-rose-950/40 dark:text-rose-300',
  },
  [VOUCHER_STATUSES.EXHAUSTED]: {
    text: 'Hết lượt',
    color: 'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
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
          <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={40} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-6 md:grid-cols-2 xl:grid-cols-4">
          {vouchers.map((v) => {
            const status = getStatusInfo(v);
            return (
              <div
                key={v.id}
                className="flex h-full flex-col rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40"
              >
                <div className="flex justify-between items-start mb-4">
                  <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25">
                    <Ticket size={22} />
                  </div>
                  <span
                    className={`text-[10px] font-bold px-3 py-1 rounded-full uppercase tracking-wider ${status.color}`}
                  >
                    {status.text}
                  </span>
                </div>

                <h3 className="mb-1 text-lg font-black font-mono uppercase tracking-wider text-slate-900 dark:text-white">
                  {v.code}
                </h3>
                <div className="mb-4 text-2xl font-black font-mono tracking-tight text-indigo-600 dark:text-indigo-400">
                  {v.discountPercent ? `${v.discountPercent}%` : fmtVND(v.discountAmount)}
                </div>
                {/* ... Thông tin chi tiết ... */}
                <div className="mb-6 space-y-2 text-xs text-slate-500 dark:text-slate-400">
                  <div className="flex justify-between">
                    <span>Đã dùng:</span>{' '}
                    <span className="font-bold text-slate-800 dark:text-slate-200 font-mono">
                      {v.usedCount}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span>Giới hạn:</span>{' '}
                    <span className="font-bold text-slate-800 dark:text-slate-200 font-mono">
                      {v.usageLimit === 0 ? '∞' : v.usageLimit}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span>Hạn dùng:</span>{' '}
                    <span className="font-bold text-slate-800 dark:text-slate-200">
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
        <div className="rounded-3xl border border-dashed border-slate-200/80 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800/80 dark:bg-slate-900/90 dark:text-slate-500">
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
