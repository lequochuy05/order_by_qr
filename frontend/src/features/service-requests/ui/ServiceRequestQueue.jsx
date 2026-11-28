import { useState } from 'react';
import { AlertCircle, BellRing, WifiOff } from 'lucide-react';

import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { AiNoteTriageCard } from '@entities/ai';
import {
  PLAN_FEATURES,
  UpgradeProModal,
  useSubscription,
} from '@entities/subscription';
import EmptyState from '@shared/ui/EmptyState.jsx';
import useServiceRequestsQueue from '../model/useServiceRequestsQueue.js';
import ServiceRequestCard from './ServiceRequestCard.jsx';
import ServiceRequestFilters from './ServiceRequestFilters.jsx';

const ServiceRequestQueue = () => {
  const {
    copy,
    language,
    isEn,
    currentUser,
    statusFilter,
    setStatusFilter,
    mineOnly,
    setMineOnly,
    tableIdFilter,
    clearTableFilter,
    requests,
    summary,
    nowMs,
    actingId,
    wsConnected,
    isLoading,
    isFetching,
    isError,
    error,
    handleRefresh,
    handleClaim,
    handleResolve,
    handleCancel,
  } = useServiceRequestsQueue();

  const { hasFeature } = useSubscription();
  const canUseAiOps = hasFeature(PLAN_FEATURES.AI_OPERATIONS_SUITE);
  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const isAdmin = currentUser?.role === 'ADMIN';
  const canViewBilling = isAdmin || currentUser?.role === 'MANAGER';

  return (
    <div className="w-full min-w-0 space-y-5 p-0 sm:p-3 lg:p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
            {copy.title}
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{copy.subtitle}</p>
        </div>

        <div className="grid grid-cols-3 gap-2.5 sm:gap-3">
          <div className="rounded-2xl border border-amber-200/80 bg-amber-50/70 px-3.5 py-2.5 dark:border-amber-500/30 dark:bg-amber-950/20">
            <p className="text-[10px] font-black uppercase tracking-wider text-amber-700 dark:text-amber-300">
              {copy.summaryPending}
            </p>
            <p className="mt-0.5 text-xl font-black text-amber-900 dark:text-amber-100">
              {summary.pending}
            </p>
          </div>
          <div className="rounded-2xl border border-indigo-200/80 bg-indigo-50/70 px-3.5 py-2.5 dark:border-indigo-500/30 dark:bg-indigo-950/20">
            <p className="text-[10px] font-black uppercase tracking-wider text-indigo-700 dark:text-indigo-300">
              {copy.summaryClaimed}
            </p>
            <p className="mt-0.5 text-xl font-black text-indigo-900 dark:text-indigo-100">
              {summary.claimed}
            </p>
          </div>
          <div className="rounded-2xl border border-rose-200/80 bg-rose-50/70 px-3.5 py-2.5 dark:border-rose-500/30 dark:bg-rose-950/20">
            <p className="text-[10px] font-black uppercase tracking-wider text-rose-700 dark:text-rose-300">
              {copy.summaryOverdue}
            </p>
            <p className="mt-0.5 text-xl font-black text-rose-900 dark:text-rose-100">
              {summary.overdue}
            </p>
          </div>
        </div>
      </div>

      {!wsConnected && (
        <div className="flex items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs font-semibold text-amber-800 dark:border-amber-500/30 dark:bg-amber-950/30 dark:text-amber-300">
          <WifiOff size={15} className="shrink-0" />
          <span>{copy.liveDisconnected}</span>
        </div>
      )}

      <ServiceRequestFilters
        copy={copy}
        statusFilter={statusFilter}
        onSelectStatus={setStatusFilter}
        mineOnly={mineOnly}
        onToggleMineOnly={setMineOnly}
        tableIdFilter={tableIdFilter}
        onClearTableFilter={clearTableFilter}
        wsConnected={wsConnected}
        isFetching={isFetching}
        onRefresh={handleRefresh}
        summary={summary}
      />

      <AiNoteTriageCard
        namespace="serviceRequests"
        locked={!canUseAiOps}
        onRequestUpgrade={() => setUpgradeOpen(true)}
      />

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[1, 2, 3].map((item) => (
            <div
              key={item}
              className="h-44 animate-pulse rounded-3xl border border-slate-200 bg-slate-100/80 dark:border-slate-800 dark:bg-slate-800/50"
            />
          ))}
        </div>
      ) : isError ? (
        <div className="flex flex-col items-center justify-center rounded-3xl border border-rose-200 bg-rose-50/60 p-8 text-center dark:border-rose-500/30 dark:bg-rose-950/20">
          <AlertCircle size={32} className="mb-3 text-rose-600 dark:text-rose-400" />
          <p className="text-sm font-bold text-rose-800 dark:text-rose-200">{copy.loadError}</p>
          <p className="mt-1 text-xs text-rose-600 dark:text-rose-300">
            {buildErrorMessage(error, { includeDetails: false })}
          </p>
          <button
            type="button"
            onClick={handleRefresh}
            className="mt-4 rounded-xl bg-rose-600 px-4 py-2 text-xs font-bold text-white transition hover:bg-rose-500"
          >
            {isEn ? 'Retry' : 'Thử lại ngay'}
          </button>
        </div>
      ) : requests.length === 0 ? (
        <EmptyState
          icon={BellRing}
          message={
            statusFilter === 'OPEN' && !mineOnly && !tableIdFilter
              ? copy.emptyOpen
              : copy.emptyFiltered
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {requests.map((request) => (
            <ServiceRequestCard
              key={request.id}
              request={request}
              copy={copy}
              language={language}
              isEn={isEn}
              currentUser={currentUser}
              nowMs={nowMs}
              isActing={actingId === request.id}
              onClaim={handleClaim}
              onResolve={handleResolve}
              onCancel={handleCancel}
            />
          ))}
        </div>
      )}

      <UpgradeProModal
        isOpen={upgradeOpen}
        feature={PLAN_FEATURES.AI_OPERATIONS_SUITE}
        isAdmin={isAdmin}
        canViewBilling={canViewBilling}
        onClose={() => setUpgradeOpen(false)}
      />
    </div>
  );
};

export default ServiceRequestQueue;
