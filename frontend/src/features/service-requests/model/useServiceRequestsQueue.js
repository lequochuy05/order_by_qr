import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  isServiceRequestOverdue,
  SERVICE_REQUEST_STATUSES,
  sortServiceRequestQueue,
  useCancelStaffServiceRequestMutation,
  useClaimServiceRequestMutation,
  useResolveServiceRequestMutation,
  useServiceRequestsQueueQuery,
} from '@entities/service-request';
import { useAuth } from '@features/auth';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { adminWsService } from '@shared/lib/websocket.js';

const EMPTY_LIST = [];

const useServiceRequestsQueue = () => {
  const { copy, language, isEn } = useTranslation('serviceRequests');
  const { user } = useAuth();
  const { confirm } = useConfirmModal();
  const [searchParams, setSearchParams] = useSearchParams();

  const tableIdParam = searchParams.get('tableId');
  const parsedTableId =
    tableIdParam && !Number.isNaN(Number(tableIdParam)) ? Number(tableIdParam) : null;

  const [statusFilter, setStatusFilter] = useState('OPEN');
  const [mineOnly, setMineOnly] = useState(false);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [actingId, setActingId] = useState(null);
  const [wsConnected, setWsConnected] = useState(() => adminWsService.isConnected());

  useEffect(() => {
    const unsubscribe = adminWsService.addConnectListener((connected) => {
      setWsConnected(connected);
    });
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 15 * 1000);

    return () => {
      unsubscribe();
      window.clearInterval(timer);
    };
  }, []);

  const queueQuery = useServiceRequestsQueueQuery({
    status: statusFilter,
    mineOnly,
    tableId: parsedTableId,
  });

  const openSummaryQuery = useServiceRequestsQueueQuery({
    status: 'OPEN',
    mineOnly: false,
    tableId: null,
  });

  const claimMutation = useClaimServiceRequestMutation();
  const resolveMutation = useResolveServiceRequestMutation();
  const cancelMutation = useCancelStaffServiceRequestMutation();

  const requests = useMemo(
    () => sortServiceRequestQueue(queueQuery.data ?? EMPTY_LIST, statusFilter),
    [queueQuery.data, statusFilter],
  );

  const openQueue = openSummaryQuery.data ?? EMPTY_LIST;
  const summary = useMemo(() => {
    let pending = 0;
    let claimed = 0;
    let overdue = 0;
    openQueue.forEach((req) => {
      if (req.status === SERVICE_REQUEST_STATUSES.PENDING) pending += 1;
      if (req.status === SERVICE_REQUEST_STATUSES.CLAIMED) claimed += 1;
      if (isServiceRequestOverdue(req, nowMs)) overdue += 1;
    });
    return {
      pending,
      claimed,
      overdue,
      openTotal: pending + claimed,
    };
  }, [nowMs, openQueue]);

  const handleRefresh = useCallback(async () => {
    await Promise.allSettled([queueQuery.refetch(), openSummaryQuery.refetch()]);
  }, [openSummaryQuery, queueQuery]);

  const clearTableFilter = useCallback(() => {
    const next = new URLSearchParams(searchParams);
    next.delete('tableId');
    setSearchParams(next);
  }, [searchParams, setSearchParams]);

  const handleClaim = useCallback(
    async (requestId) => {
      if (!requestId || actingId) return;
      setActingId(requestId);
      try {
        await claimMutation.mutateAsync(requestId);
        showSuccessToast(copy.claimSuccess);
      } catch (err) {
        showErrorToast(err);
      } finally {
        setActingId(null);
      }
    },
    [actingId, claimMutation, copy.claimSuccess],
  );

  const handleResolve = useCallback(
    async (requestId) => {
      if (!requestId || actingId) return;
      setActingId(requestId);
      try {
        await resolveMutation.mutateAsync(requestId);
        showSuccessToast(copy.resolveSuccess);
      } catch (err) {
        showErrorToast(err);
      } finally {
        setActingId(null);
      }
    },
    [actingId, copy.resolveSuccess, resolveMutation],
  );

  const handleCancel = useCallback(
    async (request) => {
      if (!request?.id || actingId) return;
      const confirmed = await confirm(copy.cancelConfirmTitle, copy.cancelConfirmMessage);
      if (!confirmed) return;

      setActingId(request.id);
      try {
        await cancelMutation.mutateAsync({ id: request.id });
        showSuccessToast(copy.cancelSuccess);
      } catch (err) {
        showErrorToast(err);
      } finally {
        setActingId(null);
      }
    },
    [actingId, cancelMutation, confirm, copy.cancelConfirmMessage, copy.cancelConfirmTitle, copy.cancelSuccess],
  );

  return {
    copy,
    language,
    isEn,
    currentUser: user,
    statusFilter,
    setStatusFilter,
    mineOnly,
    setMineOnly,
    tableIdFilter: parsedTableId,
    clearTableFilter,
    requests,
    summary,
    nowMs,
    actingId,
    wsConnected,
    isLoading: queueQuery.isLoading,
    isFetching: queueQuery.isFetching,
    isError: queueQuery.isError,
    error: queueQuery.error,
    handleRefresh,
    handleClaim,
    handleResolve,
    handleCancel,
  };
};

export default useServiceRequestsQueue;
