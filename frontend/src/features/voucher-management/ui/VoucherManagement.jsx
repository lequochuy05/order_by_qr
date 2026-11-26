import React, { useState } from 'react';
import { Loader2, Ticket } from 'lucide-react';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useCrudModal } from '@shared/hooks/useCrudModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import {
  useCreateVoucherMutation,
  useDeleteVoucherMutation,
  useUpdateVoucherMutation,
  useVoucherPageQuery,
} from '@entities/voucher';
import { VOUCHER_STATUSES } from '@entities/voucher';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import EditDeleteActions from '@shared/ui/EditDeleteActions.jsx';
import VoucherModal from './VoucherModal.jsx';
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
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 24;

  const crud = useCrudModal();
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

  const handleSearchChange = (value) => {
    setSearchTerm(value);
    setCurrentPage(0);
  };

  const handleStatusFilterChange = (value) => {
    setStatusFilter(value);
    setCurrentPage(0);
  };

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

  const handleSubmit = async (data) => {
    crud.setSubmitting(true);
    const safeValidFrom =
      data.validFrom && !data.validFrom.includes('T')
        ? `${data.validFrom}T00:00:00`
        : data.validFrom;
    const safeValidTo =
      data.validTo && !data.validTo.includes('T') ? `${data.validTo}T23:59:59` : data.validTo;

    const parsedUsageLimit =
      data.usageLimit === null || data.usageLimit === undefined || data.usageLimit === '' || data.usageLimit === 0
        ? null
        : parseInt(data.usageLimit, 10) || null;

    const payload = {
      ...data,
      code: data.code.trim().toUpperCase(),
      type: data.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
      discountPercent: data.discountPercent ? parseFloat(data.discountPercent) : null,
      discountAmount: data.discountAmount ? parseFloat(data.discountAmount) : null,
      usageLimit: parsedUsageLimit,
      validFrom: safeValidFrom,
      validTo: safeValidTo,
    };

    try {
      if (crud.editingItem?.id) {
        await updateVoucher({ id: crud.editingItem.id, data: payload });
        showSuccessToast(`Đã cập nhật voucher`);
      } else {
        await createVoucher(payload);
        showSuccessToast(`Đã thêm voucher`);
      }
      crud.close();
    } catch (err) {
      const errorMsg = err.message || '';
      if (err?.code === 'VOUCHER_CODE_EXISTS' || errorMsg.includes('Voucher code already exists')) {
        crud.setFormErrors({ code: 'Mã voucher này đã tồn tại trên hệ thống' });
      } else {
        showErrorToast(err);
      }
    } finally {
      crud.setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (crud.deletingId) return;
    const confirmed = await confirm('Xóa voucher', 'Bạn chắc chắn muốn xóa voucher này?');
    if (!confirmed) return;
    crud.setDeletingId(id);
    try {
      await deleteVoucher(id);
      showSuccessToast('Đã xóa voucher thành công');
    } catch (err) {
      showErrorToast(err);
    } finally {
      crud.setDeletingId(null);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm mã voucher..."
        searchTerm={searchTerm}
        setSearchTerm={handleSearchChange}
        onAddClick={() => crud.openCreate()}
        addButtonText="Thêm Voucher"
        showFilter
        filterAllLabel="Tất cả trạng thái"
        filterValue={statusFilter}
        setFilterValue={handleStatusFilterChange}
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
                  onEdit={() => crud.openEdit(v)}
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
        key={crud.isOpen ? crud.editingItem?.id || 'new' : 'closed'}
        isOpen={crud.isOpen}
        onClose={crud.close}
        initialData={crud.editingItem}
        onSubmit={handleSubmit}
        isSubmitting={crud.isSubmitting}
        serverErrors={crud.formErrors}
        onClearServerError={crud.clearFormError}
      />
    </div>
  );
};

export default VoucherManager;
