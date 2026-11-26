import {
  AlertCircle,
  BellRing,
  CheckCircle2,
  Clock3,
  Droplets,
  HandHelping,
  Loader2,
  MessageSquarePlus,
  ReceiptText,
  Sparkles,
  Utensils,
  WifiOff,
  X,
} from 'lucide-react';

import {
  getServiceRequestReasonMeta,
  MAX_SERVICE_REQUEST_NOTE_LENGTH,
  SERVICE_REQUEST_REASONS,
  SERVICE_REQUEST_STATUSES,
} from '@entities/service-request';
import SharedModal from '@shared/ui/SharedModal.jsx';

const REASON_ICONS = {
  Droplets,
  Utensils,
  Sparkles,
  HandHelping,
  ReceiptText,
  MessageSquarePlus,
  BellRing,
};

const renderReasonIcon = (iconName, size = 15) => {
  const Icon = REASON_ICONS[iconName] || BellRing;
  return <Icon size={size} className="shrink-0" />;
};

const formatTimeShort = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

const CallStaffModal = ({
  isOpen,
  onClose,
  selectedReason,
  onSelectReason,
  note,
  onChangeNote,
  validationError,
  openRequests = [],
  recentClosedRequests = [],
  activeReasons = [],
  openCount = 0,
  maxOpenRequests = 3,
  cooldownSeconds = 0,
  isSubmitting = false,
  cancellingId = null,
  wsConnected = true,
  onSubmit,
  onCancelRequest,
}) => {
  const activeReasonSet = new Set(activeReasons);
  const reachedOpenLimit = openCount >= maxOpenRequests;
  const allReasonsActive = SERVICE_REQUEST_REASONS.every((reason) =>
    activeReasonSet.has(reason.code),
  );
  const submitDisabled =
    isSubmitting ||
    cooldownSeconds > 0 ||
    reachedOpenLimit ||
    allReasonsActive ||
    activeReasonSet.has(selectedReason);

  const visibleRequests = [...openRequests, ...recentClosedRequests];

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      placement="bottom"
      closeOnBackdrop={true}
      ariaLabel="Gọi nhân viên hỗ trợ"
      className="flex max-h-[90dvh] flex-col overflow-hidden bg-white dark:bg-slate-900"
    >
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
            <BellRing size={18} />
          </div>
          <div>
            <h2 className="text-base font-black text-slate-900 dark:text-white">
              Gọi nhân viên hỗ trợ
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Chọn nhu cầu bên dưới để phục vụ đến hỗ trợ ngay.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng hộp thoại gọi nhân viên"
          className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        >
          <X size={18} />
        </button>
      </div>

      <div className="flex-1 space-y-4 overflow-y-auto px-5 py-4">
        {!wsConnected && (
          <div className="flex items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-3.5 py-2.5 text-xs font-semibold text-amber-800 dark:border-amber-500/30 dark:bg-amber-950/30 dark:text-amber-300">
            <WifiOff size={15} className="shrink-0" />
            <span>Kết nối trực tiếp đang gián đoạn. Vui lòng kéo làm mới để cập nhật trạng thái.</span>
          </div>
        )}

        {/* Active / recent requests tracker */}
        {visibleRequests.length > 0 && (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
                Yêu cầu của bàn ({openCount}/{maxOpenRequests} đang mở)
              </span>
            </div>
            <div className="space-y-2">
              {visibleRequests.map((req) => {
                const reasonMeta = getServiceRequestReasonMeta(req.reason, 'vi');
                const isPending = req.status === SERVICE_REQUEST_STATUSES.PENDING;
                const isClaimed = req.status === SERVICE_REQUEST_STATUSES.CLAIMED;
                const isResolved = req.status === SERVICE_REQUEST_STATUSES.RESOLVED;

                return (
                  <div
                    key={req.id}
                    className={`flex items-start justify-between gap-3 rounded-2xl border p-3 text-xs ${
                      isPending
                        ? 'border-amber-200 bg-amber-50/70 dark:border-amber-500/30 dark:bg-amber-950/20'
                        : isClaimed
                          ? 'border-indigo-200 bg-indigo-50/70 dark:border-indigo-500/30 dark:bg-indigo-950/20'
                          : isResolved
                            ? 'border-emerald-200 bg-emerald-50/60 dark:border-emerald-500/30 dark:bg-emerald-950/20'
                            : 'border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-1.5 font-bold text-slate-900 dark:text-white">
                        {renderReasonIcon(reasonMeta.icon, 14)}
                        <span className="truncate">{reasonMeta.label}</span>
                        {req.createdAt && (
                          <span className="text-[10px] font-medium text-slate-400">
                            • {formatTimeShort(req.createdAt)}
                          </span>
                        )}
                      </div>
                      {req.note && (
                        <p className="mt-1 line-clamp-2 text-[11px] text-slate-600 dark:text-slate-300">
                          “{req.note}”
                        </p>
                      )}
                      <p
                        className={`mt-1.5 flex items-center gap-1 text-[11px] font-semibold ${
                          isPending
                            ? 'text-amber-700 dark:text-amber-300'
                            : isClaimed
                              ? 'text-indigo-700 dark:text-indigo-300'
                              : isResolved
                                ? 'text-emerald-700 dark:text-emerald-300'
                                : 'text-slate-500 dark:text-slate-400'
                        }`}
                      >
                        {isPending && (
                          <>
                            <Clock3 size={12} />
                            <span>Đang chờ nhân viên tiếp nhận...</span>
                          </>
                        )}
                        {isClaimed && (
                          <>
                            <CheckCircle2 size={12} />
                            <span>
                              {req.claimedByName
                                ? `Nhân viên ${req.claimedByName} đang đến bàn bạn.`
                                : 'Nhân viên đang đến hỗ trợ bàn bạn.'}
                            </span>
                          </>
                        )}
                        {isResolved && (
                          <>
                            <CheckCircle2 size={12} />
                            <span>Nhân viên đã hỗ trợ xong.</span>
                          </>
                        )}
                        {!isPending && !isClaimed && !isResolved && (
                          <span>Yêu cầu đã được hủy.</span>
                        )}
                      </p>
                    </div>
                    {isPending && (
                      <button
                        type="button"
                        onClick={() => onCancelRequest(req.id)}
                        disabled={cancellingId === req.id}
                        className="shrink-0 rounded-xl border border-rose-200 bg-white px-2.5 py-1.5 text-[11px] font-bold text-rose-600 transition hover:bg-rose-50 active:scale-95 disabled:opacity-50 dark:border-rose-500/30 dark:bg-slate-900 dark:text-rose-400"
                      >
                        {cancellingId === req.id ? 'Đang hủy...' : 'Hủy'}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Quick reasons */}
        <div>
          <label className="mb-2 block text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Chọn lý do nhanh
          </label>
          <div className="grid grid-cols-2 gap-2">
            {SERVICE_REQUEST_REASONS.map((reason) => {
              const isSelected = selectedReason === reason.code;
              const alreadyOpen = activeReasonSet.has(reason.code);

              return (
                <button
                  key={reason.code}
                  type="button"
                  disabled={alreadyOpen || isSubmitting}
                  onClick={() => onSelectReason(reason.code)}
                  className={`flex items-center justify-between gap-2 rounded-2xl border px-3 py-2.5 text-left text-xs font-bold transition-all active:scale-98 disabled:cursor-not-allowed disabled:opacity-55 ${
                    isSelected && !alreadyOpen
                      ? 'border-indigo-600 bg-indigo-50 text-indigo-700 shadow-xs dark:border-indigo-400 dark:bg-indigo-950/60 dark:text-indigo-200'
                      : 'border-slate-200 bg-slate-50/70 text-slate-700 hover:border-slate-300 dark:border-slate-700/80 dark:bg-slate-800/60 dark:text-slate-200'
                  }`}
                >
                  <span className="flex min-w-0 items-center gap-2">
                    {renderReasonIcon(reason.icon, 15)}
                    <span className="truncate">{reason.labelVi}</span>
                  </span>
                  {alreadyOpen && (
                    <span className="shrink-0 rounded-md bg-amber-100 px-1.5 py-0.5 text-[9px] font-black uppercase text-amber-700 dark:bg-amber-900/50 dark:text-amber-300">
                      Đang chờ
                    </span>
                  )}
                </button>
              );
            })}
          </div>
          {selectedReason === 'REQUEST_BILL' && (
            <p className="mt-2 rounded-xl bg-slate-50 px-3 py-2 text-[11px] font-medium text-slate-600 dark:bg-slate-800/60 dark:text-slate-300">
              Nhân viên sẽ tới bàn hỗ trợ thanh toán (không tự khóa việc gọi thêm món).
            </p>
          )}
        </div>

        {/* Optional / Required note */}
        <div>
          <div className="mb-1.5 flex items-center justify-between text-xs">
            <label
              htmlFor="service-request-note"
              className="font-bold text-slate-700 dark:text-slate-200"
            >
              Ghi chú thêm{' '}
              <span className="font-normal text-slate-400">
                {selectedReason === 'OTHER'
                  ? '(bắt buộc tối thiểu 2 ký tự)'
                  : '(tùy chọn)'}
              </span>
            </label>
            <span className="font-mono text-[11px] text-slate-400">
              {note.length}/{MAX_SERVICE_REQUEST_NOTE_LENGTH}
            </span>
          </div>
          <textarea
            id="service-request-note"
            rows={2}
            maxLength={MAX_SERVICE_REQUEST_NOTE_LENGTH}
            value={note}
            onChange={(event) => onChangeNote(event.target.value)}
            placeholder="Ví dụ: Xin thêm 2 chén và ít khăn giấy..."
            className={`w-full resize-none rounded-2xl border bg-slate-50/70 px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:ring-2 dark:bg-slate-950/70 dark:text-slate-100 ${
              validationError
                ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-500/20'
                : 'border-slate-200 focus:border-indigo-500 focus:ring-indigo-500/20 dark:border-slate-700'
            }`}
          />
          {validationError && (
            <p className="mt-1.5 flex items-center gap-1 text-xs font-semibold text-rose-600 dark:text-rose-400">
              <AlertCircle size={13} className="shrink-0" />
              <span>{validationError}</span>
            </p>
          )}
        </div>

        {reachedOpenLimit && (
          <div className="flex items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-3.5 py-2.5 text-xs font-semibold text-amber-800 dark:border-amber-500/30 dark:bg-amber-950/30 dark:text-amber-300">
            <AlertCircle size={15} className="shrink-0" />
            <span>Bàn đang có tối đa {maxOpenRequests} yêu cầu chờ xử lý.</span>
          </div>
        )}
      </div>

      <div className="border-t border-slate-100 px-5 py-3.5 dark:border-slate-800">
        <button
          type="button"
          disabled={submitDisabled}
          onClick={onSubmit}
          className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-indigo-600/25 transition hover:from-indigo-500 hover:to-violet-500 active:scale-99 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              <span>Đang gửi yêu cầu...</span>
            </>
          ) : cooldownSeconds > 0 ? (
            <>
              <Clock3 size={16} />
              <span>Gửi lại sau {cooldownSeconds}s</span>
            </>
          ) : (
            <>
              <BellRing size={16} />
              <span>Gửi yêu cầu ngay</span>
            </>
          )}
        </button>
      </div>
    </SharedModal>
  );
};

export default CallStaffModal;
