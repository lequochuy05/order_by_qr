import React, { useEffect, useMemo, useState } from 'react';
import { BadgePercent, CalendarDays, Clock3, Loader2 } from 'lucide-react';
import { useAuth } from '@features/auth';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { useTranslation } from '@shared/lib/i18n';
import { promotionProposalToDraft } from '@shared/lib/aiMerchandisingDrafts.js';
import { getErrorDetails } from '@shared/lib/errorMessages.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import EditDeleteActions from '@shared/ui/EditDeleteActions.jsx';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import AiPromotionSuggestionsPanel from './AiPromotionSuggestionsPanel.jsx';
import {
  PROMOTION_STATUSES,
  formatPromotionTime,
  isEveryDayPromotion,
  resolvePromotionStatus,
  sortDaysOfWeek,
  useCreatePromotionMutation,
  useDeletePromotionMutation,
  usePromotionPageQuery,
  useUpdatePromotionMutation,
} from '@entities/promotion';
import PromotionModal from './PromotionModal.jsx';

const STATUS_BADGE_CLASSES = {
  [PROMOTION_STATUSES.RUNNING]:
    'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
  [PROMOTION_STATUSES.SCHEDULED]:
    'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
  [PROMOTION_STATUSES.INACTIVE]:
    'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400',
};

const formatDiscountPercent = (value) => {
  const num = Number(value);
  if (Number.isNaN(num)) return value;
  return Number.isInteger(num) ? `${num}%` : `${num}%`;
};

const PromotionManagement = () => {
  const { user } = useAuth();
  const canManage = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const { copy } = useTranslation('promotions');
  const { confirm } = useConfirmModal();

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPromotion, setEditingPromotion] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [formErrors, setFormErrors] = useState({});
  const pageSize = 24;

  const statusMetaMap = useMemo(
    () => ({
      [PROMOTION_STATUSES.RUNNING]: {
        text: copy.statusRunning,
        color: STATUS_BADGE_CLASSES[PROMOTION_STATUSES.RUNNING],
      },
      [PROMOTION_STATUSES.SCHEDULED]: {
        text: copy.statusScheduled,
        color: STATUS_BADGE_CLASSES[PROMOTION_STATUSES.SCHEDULED],
      },
      [PROMOTION_STATUSES.INACTIVE]: {
        text: copy.statusInactive,
        color: STATUS_BADGE_CLASSES[PROMOTION_STATUSES.INACTIVE],
      },
    }),
    [copy],
  );

  const statusFilterOptions = useMemo(
    () =>
      Object.entries(statusMetaMap).map(([id, meta]) => ({
        id,
        name: meta.text,
      })),
    [statusMetaMap],
  );

  const debouncedSearchTerm = useDebouncedValue(searchTerm);
  const queryParams = {
    page: currentPage,
    size: pageSize,
    ...(debouncedSearchTerm.trim() ? { q: debouncedSearchTerm.trim() } : {}),
    ...(statusFilter !== 'ALL' ? { status: statusFilter } : {}),
  };

  const { data: promotionData, isFetching: loading } = usePromotionPageQuery(queryParams);
  const promotions = promotionData?.content || [];
  const totalPages = promotionData?.totalPages || 0;
  const totalElements = promotionData?.totalElements || 0;

  const { mutateAsync: createPromotion } = useCreatePromotionMutation();
  const { mutateAsync: updatePromotion } = useUpdatePromotionMutation();
  const { mutateAsync: deletePromotion } = useDeletePromotionMutation();

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, statusFilter]);

  const handleSubmit = async (payload) => {
    if (isSaving) return;
    setIsSaving(true);

    try {
      if (editingPromotion?.id) {
        await updatePromotion({ id: editingPromotion.id, data: payload });
        showSuccessToast(copy.updateSuccess);
      } else {
        await createPromotion(payload);
        showSuccessToast(copy.createSuccess);
      }
      setFormErrors({});
      setIsModalOpen(false);
    } catch (err) {
      const errorMsg = err?.message || '';
      if (
        err?.code === 'PROMOTION_NAME_EXISTS' ||
        errorMsg.includes('Promotion name already exists')
      ) {
        setFormErrors({ name: copy.errNameExists });
      } else {
        const details = getErrorDetails(err);
        if (Object.keys(details).length > 0) {
          setFormErrors(details);
        }
        showErrorToast(err);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (deletingId) return;
    const confirmed = await confirm(copy.deleteTitle, copy.deleteConfirm);
    if (!confirmed) return;

    setDeletingId(id);
    try {
      await deletePromotion(id);
      showSuccessToast(copy.deleteSuccess);
    } catch (err) {
      showErrorToast(err);
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder={copy.searchPlaceholder}
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={() => {
          setEditingPromotion(null);
          setFormErrors({});
          setIsModalOpen(true);
        }}
        addButtonText={copy.addButton}
        hideAddButton={!canManage}
        showFilter
        filterAllLabel={copy.allStatuses}
        filterValue={statusFilter}
        setFilterValue={setStatusFilter}
        filterOptions={statusFilterOptions}
      />

      {canManage && (
        <AiPromotionSuggestionsPanel
          onUseDraft={(proposal, index) => {
            setEditingPromotion(promotionProposalToDraft(proposal, index));
            setFormErrors({});
            setIsModalOpen(true);
          }}
        />
      )}

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={40} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-6 md:grid-cols-2 xl:grid-cols-4">
          {promotions.map((promotion) => {
            const statusKey = resolvePromotionStatus(promotion);
            const statusMeta = statusMetaMap[statusKey] || statusMetaMap[PROMOTION_STATUSES.INACTIVE];
            const sortedDays = sortDaysOfWeek(promotion.daysOfWeek);
            const everyDay = isEveryDayPromotion(sortedDays);

            return (
              <div
                key={promotion.id}
                className="flex h-full flex-col rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40"
              >
                <div className="mb-4 flex items-start justify-between gap-2">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
                    <BadgePercent size={22} />
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wider ${statusMeta.color}`}
                  >
                    {statusMeta.text}
                  </span>
                </div>

                <h3
                  className="mb-1 line-clamp-2 text-base font-black tracking-tight text-slate-900 dark:text-white"
                  title={promotion.name}
                >
                  {promotion.name}
                </h3>

                <div className="mb-4 text-2xl font-black font-mono tracking-tight text-indigo-600 dark:text-indigo-400">
                  -{formatDiscountPercent(promotion.discountPercent)}
                </div>

                <div className="mb-6 space-y-3 text-xs text-slate-500 dark:text-slate-400">
                  <div className="flex items-center justify-between gap-2">
                    <span className="inline-flex items-center gap-1.5">
                      <Clock3 size={14} className="shrink-0 text-slate-400" />
                      {copy.timeSlot}:
                    </span>
                    <span className="font-mono font-bold text-slate-800 dark:text-slate-200">
                      {formatPromotionTime(promotion.startTime)} -{' '}
                      {formatPromotionTime(promotion.endTime)}
                    </span>
                  </div>

                  <div>
                    <div className="mb-2 flex items-center gap-1.5">
                      <CalendarDays size={14} className="shrink-0 text-slate-400" />
                      <span>{copy.activeDays}:</span>
                    </div>
                    {everyDay ? (
                      <span className="inline-flex rounded-xl bg-indigo-50 px-2.5 py-1 text-[11px] font-bold text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                        {copy.everyDay}
                      </span>
                    ) : (
                      <div className="flex flex-wrap gap-1.5">
                        {sortedDays.map((day) => (
                          <span
                            key={day}
                            className="rounded-lg bg-slate-100 px-2 py-0.5 text-[11px] font-bold text-slate-700 dark:bg-slate-800 dark:text-slate-200"
                          >
                            {copy[`dayShort${day}`]}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {canManage && (
                  <EditDeleteActions
                    editLabel={copy.editLabel}
                    deleting={deletingId === promotion.id}
                    onEdit={() => {
                      setEditingPromotion(promotion);
                      setFormErrors({});
                      setIsModalOpen(true);
                    }}
                    onDelete={() => handleDelete(promotion.id)}
                  />
                )}
              </div>
            );
          })}
        </div>
      )}

      {!loading && promotions.length === 0 && (
        <div className="rounded-3xl border border-dashed border-slate-200/80 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800/80 dark:bg-slate-900/90 dark:text-slate-500">
          <BadgePercent size={40} className="mx-auto mb-4 opacity-30" />
          {copy.emptyState}
        </div>
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel={copy.itemLabel}
        loading={loading}
        onPageChange={setCurrentPage}
      />

      <PromotionModal
        key={isModalOpen ? editingPromotion?.id || editingPromotion?._draftKey || 'new' : 'closed'}
        isOpen={isModalOpen}
        onClose={() => {
          setFormErrors({});
          setIsModalOpen(false);
        }}
        initialData={editingPromotion}
        onSubmit={handleSubmit}
        isSubmitting={isSaving}
        serverErrors={formErrors}
        onClearServerError={(field) =>
          setFormErrors((current) => ({ ...current, [field]: null }))
        }
      />
    </div>
  );
};

export default PromotionManagement;
