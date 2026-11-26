import {
  AlertTriangle,
  BellRing,
  CheckCircle2,
  Clock3,
  Droplets,
  HandHelping,
  Loader2,
  MessageSquarePlus,
  ReceiptText,
  Sparkles,
  UserCheck,
  Utensils,
  XCircle,
} from 'lucide-react';

import {
  getServiceRequestReasonMeta,
  getServiceRequestStatusMeta,
  getServiceRequestWaitMinutes,
  isServiceRequestOverdue,
  SERVICE_REQUEST_STATUSES,
} from '@entities/service-request';
import { fmtDateTime } from '@shared/lib/formatters.js';
import { analyzeCustomerNote, RISK_LEVELS } from '@entities/ai';
import { getStatusClasses } from '@shared/lib/statusClasses.js';
import { NoteSafetyBadges } from '@entities/ai';

const REASON_ICONS = {
  Droplets,
  Utensils,
  Sparkles,
  HandHelping,
  ReceiptText,
  MessageSquarePlus,
  BellRing,
};

const formatWaitLabel = (request, nowMs, copy) => {
  const mins = getServiceRequestWaitMinutes(request, nowMs);
  if (mins < 1) return copy.justNow;
  if (mins < 60) return `${mins} ${copy.minutesAgo}`;
  const hours = Math.floor(mins / 60);
  return `${hours} ${copy.hoursAgo}`;
};

const formatTableDisplay = (request, isEn) => {
  const raw = request.tableDisplayName || (request.tableNumber ? `Table ${request.tableNumber}` : '');
  if (!raw) return isEn ? 'Table' : 'Bàn';
  if (isEn) return raw;
  return raw.startsWith('Table ') ? raw.replace(/^Table /, 'Bàn ') : raw;
};

const ServiceRequestCard = ({
  request,
  copy,
  language,
  isEn,
  currentUser,
  nowMs,
  isActing,
  onClaim,
  onResolve,
  onCancel,
}) => {
  const reasonMeta = getServiceRequestReasonMeta(request.reason, language);
  const statusMeta = getServiceRequestStatusMeta(request.status, language);
  const overdue = isServiceRequestOverdue(request, nowMs);
  const ReasonIcon = REASON_ICONS[reasonMeta.icon] || BellRing;

  const isPending = request.status === SERVICE_REQUEST_STATUSES.PENDING;
  const isClaimed = request.status === SERVICE_REQUEST_STATUSES.CLAIMED;
  const isManagerOrAdmin =
    currentUser?.role === 'ADMIN' || currentUser?.role === 'MANAGER';
  const isAssignedToCurrentUser =
    request.claimedByUserId != null &&
    currentUser?.id != null &&
    String(request.claimedByUserId) === String(currentUser.id);
  const canManageClaimed = isManagerOrAdmin || isAssignedToCurrentUser;

  const noteSafety = analyzeCustomerNote(request.note);
  const hasAllergyNote = noteSafety.riskLevel === RISK_LEVELS.CRITICAL;

  const cardSurfaceClass =
    overdue || hasAllergyNote
      ? 'border-2 border-rose-500 bg-rose-50/40 shadow-md shadow-rose-500/10 ring-2 ring-rose-500/15 dark:border-rose-500/80 dark:bg-rose-950/20'
      : isPending
        ? 'border border-amber-300/80 bg-white/95 shadow-sm dark:border-amber-500/40 dark:bg-slate-900/95'
        : isClaimed
          ? 'border border-indigo-300/80 bg-indigo-50/30 shadow-sm dark:border-indigo-500/40 dark:bg-slate-900/95'
          : 'border border-slate-200/80 bg-white/95 dark:border-slate-800 dark:bg-slate-900/95';

  return (
    <div className={`flex flex-col justify-between rounded-3xl p-4 transition-all ${cardSurfaceClass}`}>
      <div>
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="truncate text-lg font-black tracking-tight text-slate-900 dark:text-white">
              {formatTableDisplay(request, isEn)}
            </h3>
            <p className="mt-0.5 flex items-center gap-1.5 text-xs font-semibold text-slate-500 dark:text-slate-400">
              <Clock3 size={12} className="shrink-0" />
              <span>{formatWaitLabel(request, nowMs, copy)}</span>
              {request.createdAt && (
                <span className="text-[11px] opacity-75">• {fmtDateTime(request.createdAt)}</span>
              )}
            </p>
          </div>

          <div className="flex shrink-0 flex-col items-end gap-1">
            <span
              className={`rounded-lg border px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider ${getStatusClasses(
                statusMeta.color,
              )}`}
            >
              {statusMeta.label}
            </span>
            {overdue && (
              <span className="inline-flex items-center gap-1 rounded-lg border border-rose-300 bg-rose-100 px-2 py-0.5 text-[10px] font-black uppercase text-rose-700 dark:border-rose-500/40 dark:bg-rose-950/60 dark:text-rose-300">
                <AlertTriangle size={11} />
                <span>{copy.overdueBadge}</span>
              </span>
            )}
          </div>
        </div>

        <div className="mt-3 rounded-2xl border border-slate-100 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-800/50">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-slate-100">
            <ReasonIcon size={16} className="shrink-0 text-indigo-600 dark:text-indigo-400" />
            <span>{reasonMeta.label}</span>
          </div>
          {request.note && (
            <div className="mt-1.5 space-y-1.5">
              <p className="text-xs leading-relaxed text-slate-600 dark:text-slate-300">
                “{request.note}”
              </p>
              <NoteSafetyBadges
                note={request.note}
                safety={noteSafety}
                language={language}
                compact
              />
            </div>
          )}
        </div>

        {request.claimedByName && (
          <p className="mt-2.5 flex items-center gap-1.5 text-xs font-semibold text-indigo-700 dark:text-indigo-300">
            <UserCheck size={13} className="shrink-0" />
            <span>
              {copy.claimedByPrefix} {request.claimedByName}
            </span>
          </p>
        )}

        {request.status === SERVICE_REQUEST_STATUSES.RESOLVED && request.resolvedByName && (
          <p className="mt-1 flex items-center gap-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
            <CheckCircle2 size={13} className="shrink-0" />
            <span>
              {copy.resolvedByPrefix} {request.resolvedByName}
            </span>
          </p>
        )}

        {request.status === SERVICE_REQUEST_STATUSES.CANCELLED && request.cancelledByType && (
          <p className="mt-1 flex items-center gap-1.5 text-xs font-medium text-slate-500 dark:text-slate-400">
            <XCircle size={13} className="shrink-0" />
            <span>
              {copy.cancelledByPrefix} {request.cancelledByType}
              {request.cancelledReason ? ` (${request.cancelledReason})` : ''}
            </span>
          </p>
        )}
      </div>

      {(isPending || (isClaimed && canManageClaimed)) && (
        <div className="mt-4 flex items-center gap-2 border-t border-slate-100 pt-3 dark:border-slate-800">
          {isPending && (
            <button
              type="button"
              disabled={isActing}
              onClick={() => onClaim(request.id)}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-3 py-2 text-xs font-black text-white shadow-sm shadow-indigo-600/20 transition hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:opacity-50"
            >
              {isActing ? <Loader2 size={14} className="animate-spin" /> : <HandHelping size={14} />}
              <span>{copy.claimAction}</span>
            </button>
          )}

          {isClaimed && canManageClaimed && (
            <button
              type="button"
              disabled={isActing}
              onClick={() => onResolve(request.id)}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-2 text-xs font-black text-white shadow-sm shadow-emerald-600/20 transition hover:bg-emerald-500 active:scale-95 disabled:opacity-50"
            >
              {isActing ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
              <span>{copy.resolveAction}</span>
            </button>
          )}

          {(isPending || (isClaimed && canManageClaimed)) && (
            <button
              type="button"
              disabled={isActing}
              onClick={() => onCancel(request)}
              className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-bold text-rose-700 transition hover:bg-rose-100 active:scale-95 disabled:opacity-50 dark:border-rose-500/30 dark:bg-rose-950/30 dark:text-rose-300 dark:hover:bg-rose-950/50"
            >
              {copy.cancelAction}
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default ServiceRequestCard;
