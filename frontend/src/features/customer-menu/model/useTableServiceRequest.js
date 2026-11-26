import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  computeCooldownRemaining,
  generateClientRequestId,
  MAX_OPEN_SERVICE_REQUESTS,
  SERVICE_REQUEST_COOLDOWN_SECONDS,
  SERVICE_REQUEST_REASONS,
  useCancelCustomerServiceRequestMutation,
  useCreateServiceRequestMutation,
  useCustomerServiceRequestsQuery,
  validateServiceRequestInput,
} from '@entities/service-request';
import { isTerminalSessionError } from '@entities/table';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';

const EMPTY_LIST = [];

const useTableServiceRequest = ({
  tableCode,
  sessionToken,
  ensureSessionToken,
  clearSessionToken,
  showSuccess,
  showError,
} = {}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedReason, setSelectedReason] = useState(SERVICE_REQUEST_REASONS[0].code);
  const [note, setNote] = useState('');
  const [validationError, setValidationError] = useState(null);
  const [clientRequestId, setClientRequestId] = useState(() => generateClientRequestId());
  const [localCooldownEndsAtMs, setLocalCooldownEndsAtMs] = useState(0);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [cancellingId, setCancellingId] = useState(null);

  const query = useCustomerServiceRequestsQuery(tableCode, sessionToken);
  const createMutation = useCreateServiceRequestMutation();
  const cancelMutation = useCancelCustomerServiceRequestMutation();

  const requests = query.data?.requests ?? EMPTY_LIST;
  const activeReasons = query.data?.activeReasons ?? EMPTY_LIST;
  const openCount = Number(query.data?.openCount ?? 0);
  const maxOpenRequests = Number(query.data?.maxOpenRequests ?? MAX_OPEN_SERVICE_REQUESTS);

  const openRequests = useMemo(
    () => requests.filter((req) => req.status === 'PENDING' || req.status === 'CLAIMED'),
    [requests],
  );
  const recentClosedRequests = useMemo(
    () =>
      requests
        .filter((req) => req.status === 'RESOLVED' || req.status === 'CANCELLED')
        .slice(0, 3),
    [requests],
  );

  const activeReasonSet = useMemo(() => new Set(activeReasons), [activeReasons]);
  const effectiveReason = useMemo(() => {
    if (!activeReasonSet.has(selectedReason)) return selectedReason;
    const firstAvailable = SERVICE_REQUEST_REASONS.find(
      (reason) => !activeReasonSet.has(reason.code),
    );
    return firstAvailable?.code || selectedReason;
  }, [activeReasonSet, selectedReason]);

  const serverCooldownEndsAtMs =
    Number(query.data?.cooldownRemainingSeconds) > 0 && query.dataUpdatedAt
      ? query.dataUpdatedAt + Number(query.data.cooldownRemainingSeconds) * 1000
      : 0;
  const effectiveCooldownEndsAtMs = Math.max(localCooldownEndsAtMs, serverCooldownEndsAtMs);
  const cooldownSeconds = computeCooldownRemaining(effectiveCooldownEndsAtMs, nowMs);

  useEffect(() => {
    if (effectiveCooldownEndsAtMs <= Date.now()) return undefined;
    const timer = window.setInterval(() => {
      const current = Date.now();
      setNowMs(current);
      if (current >= effectiveCooldownEndsAtMs) {
        window.clearInterval(timer);
      }
    }, 1000);
    return () => window.clearInterval(timer);
  }, [effectiveCooldownEndsAtMs]);

  const handleSelectReason = useCallback((reasonCode) => {
    setSelectedReason(reasonCode);
    setValidationError(null);
    setClientRequestId(generateClientRequestId());
  }, []);

  const handleChangeNote = useCallback((nextNote) => {
    setNote(nextNote);
    setValidationError(null);
    setClientRequestId(generateClientRequestId());
  }, []);

  const handleOpen = useCallback(() => {
    setValidationError(null);
    setNowMs(Date.now());
    setIsOpen(true);
    if (tableCode && sessionToken) {
      query.refetch();
    }
  }, [query, sessionToken, tableCode]);

  const handleClose = useCallback(() => {
    setIsOpen(false);
    setValidationError(null);
  }, []);

  const handleSubmit = useCallback(async () => {
    if (createMutation.isPending) return;

    const validation = validateServiceRequestInput({
      reason: effectiveReason,
      note,
    });
    if (!validation.valid) {
      setValidationError(validation.error);
      return;
    }

    if (!tableCode) {
      showError?.('Vui lòng quét mã QR trên bàn để gọi nhân viên.');
      return;
    }

    let token = sessionToken;
    if (!token && ensureSessionToken) {
      try {
        token = await ensureSessionToken();
      } catch (err) {
        showError?.(buildErrorMessage(err, { includeDetails: false }));
        return;
      }
    }

    if (!token) {
      showError?.('Không tìm thấy phiên phục vụ tại bàn. Vui lòng quét lại mã QR.');
      return;
    }

    try {
      await createMutation.mutateAsync({
        tableCode,
        payload: {
          sessionToken: token,
          reason: effectiveReason,
          note: validation.trimmedNote || null,
          clientRequestId,
        },
      });
      const current = Date.now();
      setNowMs(current);
      setLocalCooldownEndsAtMs(current + SERVICE_REQUEST_COOLDOWN_SECONDS * 1000);
      setNote('');
      setValidationError(null);
      setClientRequestId(generateClientRequestId());
      showSuccess?.('Đã gửi yêu cầu tới nhân viên. Nhân viên sẽ hỗ trợ bạn ngay!');
    } catch (err) {
      if (isTerminalSessionError(err)) {
        clearSessionToken?.();
      }
      if (err?.code === 'SERVICE_REQUEST_COOLDOWN' && Number(err?.details?.retryAfterSeconds) > 0) {
        const current = Date.now();
        setNowMs(current);
        setLocalCooldownEndsAtMs(current + Number(err.details.retryAfterSeconds) * 1000);
      }
      if (err?.details?.note) {
        setValidationError(buildErrorMessage(err, { includeDetails: false }));
        return;
      }
      showError?.(buildErrorMessage(err, { includeDetails: false }));
    }
  }, [
    clearSessionToken,
    clientRequestId,
    createMutation,
    effectiveReason,
    ensureSessionToken,
    note,
    sessionToken,
    showError,
    showSuccess,
    tableCode,
  ]);

  const handleCancelRequest = useCallback(
    async (requestId) => {
      if (!tableCode || !sessionToken || !requestId || cancellingId) return;
      setCancellingId(requestId);
      try {
        await cancelMutation.mutateAsync({
          tableCode,
          requestId,
          payload: { sessionToken },
        });
        showSuccess?.('Đã hủy yêu cầu hỗ trợ.');
      } catch (err) {
        if (isTerminalSessionError(err)) {
          clearSessionToken?.();
        }
        showError?.(buildErrorMessage(err, { includeDetails: false }));
      } finally {
        setCancellingId(null);
      }
    },
    [cancelMutation, cancellingId, clearSessionToken, sessionToken, showError, showSuccess, tableCode],
  );

  return {
    isOpen,
    openModal: handleOpen,
    closeModal: handleClose,
    selectedReason: effectiveReason,
    selectReason: handleSelectReason,
    note,
    setNote: handleChangeNote,
    validationError,
    requests,
    openRequests,
    recentClosedRequests,
    activeReasons,
    openCount,
    maxOpenRequests,
    cooldownSeconds,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
    isSubmitting: createMutation.isPending,
    cancellingId,
    handleSubmit,
    handleCancelRequest,
  };
};

export default useTableServiceRequest;
