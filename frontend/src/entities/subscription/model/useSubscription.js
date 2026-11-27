import { useCallback, useMemo } from 'react';

import { useCurrentSubscriptionQuery } from '../api/subscriptionQueries.js';
import {
  getRemainingDays,
  hasPlanFeature,
  isProActive,
} from '../lib/subscriptionHelpers.js';

export const useSubscription = (options = {}) => {
  const query = useCurrentSubscriptionQuery(options);
  const subscription = query.data || null;

  const isPro = useMemo(() => isProActive(subscription), [subscription]);
  const remainingDays = useMemo(
    () => getRemainingDays(subscription?.expiresAt),
    [subscription?.expiresAt],
  );

  const hasFeature = useCallback(
    (feature) => hasPlanFeature(subscription, feature),
    [subscription],
  );

  return {
    subscription,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
    isPro,
    planTier: subscription?.planTier || 'FREE',
    status: subscription?.status || 'ACTIVE',
    billingCycle: subscription?.billingCycle || null,
    expiresAt: subscription?.expiresAt || null,
    trialUsed: Boolean(subscription?.trialUsed),
    usage: subscription?.usage || null,
    remainingDays,
    hasFeature,
  };
};

export default useSubscription;
