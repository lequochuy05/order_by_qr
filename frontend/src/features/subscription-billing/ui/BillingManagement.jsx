import { useState } from 'react';
import {
  AlertTriangle,
  Calendar,
  CheckCircle2,
  Crown,
  Loader2,
  Lock,
  QrCode,
  Receipt,
  RefreshCw,
  Sparkles,
  Table,
  Users,
} from 'lucide-react';

import { useAuth } from '@entities/session';
import {
  BILLING_CYCLES,
  formatBillingCycleLabel,
  getUsagePercent,
  ProBadge,
  useActivateTrialMutation,
  useCreateSubscriptionCheckoutMutation,
  useSubscription,
  useSubscriptionInvoicesQuery,
  useSubscriptionPlansQuery,
  useSyncSubscriptionInvoiceMutation,
} from '@entities/subscription';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import SubscriptionQrModal from './modals/SubscriptionQrModal.jsx';

const INVOICE_STATUS_META = {
  PAID: {
    vi: 'Đã thanh toán',
    en: 'Paid',
    cls: 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300',
  },
  PENDING: {
    vi: 'Chờ thanh toán',
    en: 'Pending',
    cls: 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/30 dark:bg-amber-500/15 dark:text-amber-300',
  },
  CANCELLED: {
    vi: 'Đã hủy',
    en: 'Cancelled',
    cls: 'border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300',
  },
  EXPIRED: {
    vi: 'Hết hạn',
    en: 'Expired',
    cls: 'border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300',
  },
};

const formatDateTime = (value, isEn) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(isEn ? 'en-US' : 'vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const BillingManagement = () => {
  const { user } = useAuth();
  const { language, isEn } = useTranslation();
  const isAdmin = user?.role === 'ADMIN';

  const {
    isLoading: isSubLoading,
    isError: isSubError,
    refetch: refetchSubscription,
    isPro,
    status,
    billingCycle,
    expiresAt,
    trialUsed,
    usage,
    remainingDays,
  } = useSubscription();

  const {
    data: plans = [],
    isLoading: isPlansLoading,
    refetch: refetchPlans,
  } = useSubscriptionPlansQuery();

  const {
    data: invoices = [],
    isLoading: isInvoicesLoading,
    refetch: refetchInvoices,
  } = useSubscriptionInvoicesQuery();

  const { mutateAsync: activateTrial, isPending: isActivatingTrial } = useActivateTrialMutation();
  const { mutateAsync: createCheckout, isPending: isCreatingCheckout } =
    useCreateSubscriptionCheckoutMutation();
  const { mutateAsync: syncInvoice, isPending: isSyncingRow } =
    useSyncSubscriptionInvoiceMutation();

  const [activeInvoice, setActiveInvoice] = useState(null);
  const [checkoutCycleLoading, setCheckoutCycleLoading] = useState(null);

  const liveActiveInvoice = activeInvoice
    ? invoices.find((inv) => inv.id === activeInvoice.id) || activeInvoice
    : null;

  const handleRefreshAll = () => {
    refetchSubscription();
    refetchPlans();
    refetchInvoices();
  };

  const handleStartTrial = async () => {
    if (!isAdmin || isActivatingTrial) return;
    try {
      await activateTrial();
      showSuccessToast(
        isEn
          ? '14-day QROS Pro trial activated! All AI & Inventory BOM features are unlocked.'
          : 'Đã kích hoạt dùng thử QROS Pro 14 ngày! Toàn bộ tính năng AI & Kho BOM đã được mở khóa.',
      );
    } catch (error) {
      showErrorToast(error);
    }
  };

  const handleCheckout = async (cycle) => {
    if (!isAdmin || isCreatingCheckout) return;
    setCheckoutCycleLoading(cycle);
    try {
      const idempotencyKey = `sub-${cycle.toLowerCase()}-${Date.now()}`;
      const invoice = await createCheckout({
        billingCycle: cycle,
        returnPath: '/admin/billing',
        idempotencyKey,
      });
      setActiveInvoice(invoice);
    } catch (error) {
      showErrorToast(error);
    } finally {
      setCheckoutCycleLoading(null);
    }
  };

  const handleRowSync = async (invoiceId) => {
    try {
      const updated = await syncInvoice(invoiceId);
      if (updated?.status === 'PAID') {
        showSuccessToast(
          isEn ? 'Invoice confirmed as PAID!' : 'Đã xác nhận thanh toán thành công!',
        );
      } else {
        showSuccessToast(
          isEn ? 'Synced latest payment status.' : 'Đã đồng bộ trạng thái thanh toán mới nhất.',
        );
      }
    } catch (error) {
      showErrorToast(error);
    }
  };

  const tablesPercent = getUsagePercent(usage?.activeTables, usage?.maxTables);
  const usersPercent = getUsagePercent(usage?.activeUsers, usage?.maxUsers);

  return (
    <div className="space-y-6 pb-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
              {isEn ? 'Subscription & Billing' : 'Gói dịch vụ & Nâng cấp Pro'}
            </h1>
            {isPro && <ProBadge label={status === 'TRIALING' ? 'PRO TRIAL' : 'PRO'} />}
          </div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isEn
              ? 'Manage your QROS plan tier, resource limits, AI/BOM entitlements, and VietQR upgrade invoices.'
              : 'Quản lý gói dịch vụ QROS, hạn mức bàn/nhân sự, mở khóa AI & Kho BOM và lịch sử thanh toán VietQR.'}
          </p>
        </div>

        <button
          type="button"
          onClick={handleRefreshAll}
          className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          <RefreshCw size={15} />
          <span>{isEn ? 'Refresh' : 'Làm mới'}</span>
        </button>
      </div>

      {isSubError && (
        <div
          role="alert"
          className="flex items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-semibold text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300"
        >
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} />
            <span>
              {isEn
                ? 'Unable to load current subscription status.'
                : 'Không thể tải thông tin gói dịch vụ hiện tại.'}
            </span>
          </div>
          <button
            type="button"
            onClick={handleRefreshAll}
            className="rounded-lg bg-rose-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-rose-700"
          >
            {isEn ? 'Retry' : 'Thử lại'}
          </button>
        </div>
      )}

      {/* Current Plan & Usage Summary */}
      <section className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-xs dark:border-slate-800 dark:bg-slate-900 lg:col-span-7">
          {isSubLoading ? (
            <div className="animate-pulse space-y-4">
              <div className="h-6 w-48 rounded-lg bg-slate-200 dark:bg-slate-800" />
              <div className="h-4 w-72 rounded-lg bg-slate-200 dark:bg-slate-800" />
              <div className="h-10 w-44 rounded-xl bg-slate-200 dark:bg-slate-800" />
            </div>
          ) : (
            <div className="flex h-full flex-col justify-between gap-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex items-center gap-3.5">
                  <div
                    className={`flex h-12 w-12 items-center justify-center rounded-2xl shadow-sm ${
                      isPro
                        ? 'bg-gradient-to-tr from-amber-500 to-indigo-600 text-white'
                        : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                    }`}
                  >
                    <Crown size={22} />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-lg font-black text-slate-900 dark:text-white">
                        {isPro
                          ? formatBillingCycleLabel(billingCycle, language)
                          : isEn
                            ? 'QROS Free Plan'
                            : 'Gói QROS Miễn phí (Free)'}
                      </h2>
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold ${
                          isPro
                            ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                            : status === 'EXPIRED'
                              ? 'bg-rose-50 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300'
                              : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                        }`}
                      >
                        {isPro
                          ? status === 'TRIALING'
                            ? isEn
                              ? 'Trialing'
                              : 'Đang dùng thử'
                            : isEn
                              ? 'Active'
                              : 'Đang hoạt động'
                          : status === 'EXPIRED'
                            ? isEn
                              ? 'Pro Expired (Read-only BOM)'
                              : 'Hết hạn Pro (Chỉ đọc kho)'
                            : isEn
                              ? 'Basic Active'
                              : 'Gói cơ bản'}
                      </span>
                    </div>
                    <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
                      <Calendar size={13} />
                      {expiresAt ? (
                        <span>
                          {isEn ? 'Valid until:' : 'Hiệu lực đến:'}{' '}
                          <strong className="font-mono text-slate-700 dark:text-slate-200">
                            {formatDateTime(expiresAt, isEn)}
                          </strong>
                          {remainingDays != null && isPro && (
                            <span className="ml-1.5 font-bold text-indigo-600 dark:text-indigo-400">
                              ({remainingDays} {isEn ? 'days left' : 'ngày còn lại'})
                            </span>
                          )}
                        </span>
                      ) : (
                        <span>
                          {isEn
                            ? 'No expiration on basic operations.'
                            : 'Không giới hạn thời gian với các tính năng bán hàng cơ bản.'}
                        </span>
                      )}
                    </p>
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                {isAdmin && !isPro && !trialUsed && (
                  <button
                    type="button"
                    onClick={handleStartTrial}
                    disabled={isActivatingTrial}
                    className="inline-flex h-10 items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 text-xs font-bold text-white shadow-sm transition hover:opacity-95 disabled:opacity-50"
                  >
                    {isActivatingTrial ? (
                      <Loader2 size={15} className="animate-spin" />
                    ) : (
                      <Sparkles size={15} />
                    )}
                    <span>
                      {isEn ? 'Activate 14-Day Free Pro Trial' : 'Dùng thử Pro 14 ngày miễn phí'}
                    </span>
                  </button>
                )}

                {!isAdmin && (
                  <div className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-600 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-300">
                    <Lock size={14} className="text-slate-400" />
                    <span>
                      {isEn
                        ? 'Manager view only. Contact Admin to activate trial or pay via VietQR.'
                        : 'Chế độ xem của Quản lý. Liên hệ Chủ quán (Admin) để kích hoạt dùng thử hoặc thanh toán nâng cấp.'}
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Usage meters */}
        <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-xs dark:border-slate-800 dark:bg-slate-900 lg:col-span-5">
          <h3 className="text-sm font-black text-slate-900 dark:text-white">
            {isEn ? 'Resource Usage' : 'Mức sử dụng tài nguyên'}
          </h3>
          <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
            {isPro
              ? isEn
                ? 'Unlimited tables and staff accounts unlocked.'
                : 'Đã mở không giới hạn bàn ăn và tài khoản nhân sự.'
              : isEn
                ? 'Free plan limits active tables and staff accounts.'
                : 'Hạn mức đang áp dụng cho gói Miễn phí.'}
          </p>

          <div className="mt-4 space-y-4">
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs font-bold">
                <span className="inline-flex items-center gap-1.5 text-slate-700 dark:text-slate-200">
                  <Table size={14} className="text-indigo-500" />
                  {isEn ? 'Active Tables' : 'Bàn đang hoạt động'}
                </span>
                <span className="font-mono tabular-nums text-slate-900 dark:text-white">
                  {usage?.activeTables ?? 0} /{' '}
                  {usage?.maxTables != null ? usage.maxTables : isEn ? 'Unlimited' : 'Không giới hạn'}
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                <div
                  className={`h-full rounded-full transition-all ${
                    tablesPercent >= 90 ? 'bg-rose-500' : 'bg-indigo-600'
                  }`}
                  style={{ width: `${usage?.maxTables != null ? tablesPercent : 20}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs font-bold">
                <span className="inline-flex items-center gap-1.5 text-slate-700 dark:text-slate-200">
                  <Users size={14} className="text-violet-500" />
                  {isEn ? 'Active Staff Accounts' : 'Tài khoản nhân sự'}
                </span>
                <span className="font-mono tabular-nums text-slate-900 dark:text-white">
                  {usage?.activeUsers ?? 0} /{' '}
                  {usage?.maxUsers != null ? usage.maxUsers : isEn ? 'Unlimited' : 'Không giới hạn'}
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                <div
                  className={`h-full rounded-full transition-all ${
                    usersPercent >= 90 ? 'bg-rose-500' : 'bg-violet-600'
                  }`}
                  style={{ width: `${usage?.maxUsers != null ? usersPercent : 20}%` }}
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Plan Comparison Cards */}
      <section className="space-y-3">
        <h2 className="text-base font-black text-slate-900 dark:text-white">
          {isEn ? 'Choose Your Plan' : 'So sánh & Nâng cấp gói dịch vụ'}
        </h2>

        {isPlansLoading ? (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {[1, 2, 3].map((idx) => (
              <div
                key={idx}
                className="h-72 animate-pulse rounded-3xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
              />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {plans.map((plan) => {
              const isFreeCard = plan.planTier === 'FREE';
              const isCurrentCard =
                (isFreeCard && !isPro) ||
                (!isFreeCard && isPro && billingCycle === plan.billingCycle);

              const bullets = isFreeCard
                ? [
                    isEn
                      ? `Up to ${plan.maxTables ?? 10} active dining tables`
                      : `Tối đa ${plan.maxTables ?? 10} bàn đang hoạt động`,
                    isEn
                      ? `Up to ${plan.maxUsers ?? 5} active staff accounts`
                      : `Tối đa ${plan.maxUsers ?? 5} tài khoản nhân sự`,
                    isEn
                      ? 'QR Menu, KDS 3-stage board, Cash & VietQR at counter'
                      : 'QR Menu, Bếp KDS 3 trạng thái, Tiền mặt & VietQR tại quầy',
                    isEn
                      ? 'Existing inventory records stay read-only'
                      : 'Xem dữ liệu kho cũ ở chế độ chỉ đọc',
                  ]
                : [
                    isEn
                      ? 'All AI Operations: Insights, Restock, Invoice OCR, BOM Costing, Allergy Triage, Combos'
                      : 'Trọn bộ AI Vận hành: Tóm tắt ca, Dự báo kho, Quét hóa đơn Vision, BOM & Giá vốn, Dị ứng, Combo',
                    isEn
                      ? 'Customer AI Chat Assistant with 1-tap cart ordering'
                      : 'Trợ lý AI Khách hàng đặt món trực tiếp 1 chạm',
                    isEn
                      ? 'Full Inventory Stock-In, Batch Import & BOM Recipe editing'
                      : 'Toàn quyền Quản trị Kho, Nhập hàng loạt & Định mức BOM',
                    isEn
                      ? 'Unlimited dining tables & staff accounts'
                      : 'Không giới hạn số bàn & tài khoản nhân sự',
                  ];

              return (
                <div
                  key={plan.code}
                  className={`flex flex-col justify-between rounded-3xl border p-6 transition ${
                    plan.recommended
                      ? 'border-indigo-500/70 bg-gradient-to-b from-indigo-50/60 via-white to-white shadow-md dark:border-indigo-500/50 dark:from-indigo-950/35 dark:via-slate-900 dark:to-slate-900'
                      : 'border-slate-200/80 bg-white shadow-xs dark:border-slate-800 dark:bg-slate-900'
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="text-base font-black text-slate-900 dark:text-white">
                        {isFreeCard
                          ? isEn
                            ? 'Free Basic'
                            : 'Gói Cơ bản (Free)'
                          : plan.billingCycle === BILLING_CYCLES.YEARLY
                            ? isEn
                              ? 'Pro Yearly'
                              : 'Pro Theo Năm'
                            : isEn
                              ? 'Pro Monthly'
                              : 'Pro Theo Tháng'}
                      </h3>
                      {plan.recommended && (
                        <span className="rounded-full bg-indigo-600 px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-white">
                          {isEn ? 'Best Value' : 'Tiết kiệm nhất'}
                        </span>
                      )}
                    </div>

                    <div className="mt-4 flex items-baseline gap-1.5">
                      <span className="font-mono text-3xl font-black tabular-nums text-slate-900 dark:text-white">
                        {Number(plan.priceVnd || 0) === 0 ? (isEn ? '0đ' : 'Miễn phí') : fmtVND(plan.priceVnd)}
                      </span>
                      {plan.durationDays && (
                        <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                          / {plan.durationDays} {isEn ? 'days' : 'ngày'}
                        </span>
                      )}
                    </div>

                    <ul className="mt-5 space-y-2.5">
                      {bullets.map((text, idx) => (
                        <li
                          key={idx}
                          className="flex items-start gap-2 text-xs font-medium text-slate-600 dark:text-slate-300"
                        >
                          <CheckCircle2
                            size={15}
                            className={`mt-0.5 shrink-0 ${
                              isFreeCard ? 'text-slate-400' : 'text-emerald-500'
                            }`}
                          />
                          <span>{text}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="mt-6">
                    {isFreeCard ? (
                      <div className="flex h-10 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-xs font-bold text-slate-500 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
                        {isCurrentCard
                          ? isEn
                            ? 'Current Plan'
                            : 'Đang sử dụng'
                          : isEn
                            ? 'Included by default'
                            : 'Mặc định cơ bản'}
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleCheckout(plan.billingCycle)}
                        disabled={!isAdmin || isCreatingCheckout}
                        className={`inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl px-4 text-xs font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${
                          plan.recommended
                            ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700'
                            : 'border border-indigo-200 bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white dark:border-indigo-700/50 dark:bg-indigo-950/40 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white'
                        }`}
                      >
                        {checkoutCycleLoading === plan.billingCycle ? (
                          <Loader2 size={15} className="animate-spin" />
                        ) : (
                          <QrCode size={15} />
                        )}
                        <span>
                          {isPro
                            ? isEn
                              ? 'Extend via VietQR'
                              : 'Gia hạn thêm qua VietQR'
                            : isEn
                              ? 'Upgrade via VietQR'
                              : 'Nâng cấp qua VietQR'}
                        </span>
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Invoice History */}
      <section className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-xs dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-4 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Receipt size={18} className="text-indigo-500" />
            <h2 className="text-base font-black text-slate-900 dark:text-white">
              {isEn ? 'Upgrade Invoices' : 'Lịch sử hóa đơn gói dịch vụ'}
            </h2>
          </div>
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
            {invoices.length} {isEn ? 'record(s)' : 'giao dịch'}
          </span>
        </div>

        {isInvoicesLoading ? (
          <div className="space-y-2.5">
            {[1, 2].map((idx) => (
              <div
                key={idx}
                className="h-12 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800"
              />
            ))}
          </div>
        ) : invoices.length === 0 ? (
          <div className="rounded-2xl border-2 border-dashed border-slate-200 py-10 text-center text-xs font-medium text-slate-500 dark:border-slate-800 dark:text-slate-400">
            {isEn
              ? 'No subscription upgrade invoices yet.'
              : 'Chưa có hóa đơn thanh toán nâng cấp gói nào.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:text-slate-400">
                  <th className="py-2.5 pr-3">{isEn ? 'Invoice' : 'Mã HĐ'}</th>
                  <th className="py-2.5 pr-3">{isEn ? 'Plan' : 'Gói'}</th>
                  <th className="py-2.5 pr-3">{isEn ? 'Amount' : 'Số tiền'}</th>
                  <th className="py-2.5 pr-3">{isEn ? 'Status' : 'Trạng thái'}</th>
                  <th className="py-2.5 pr-3">{isEn ? 'Created / Paid' : 'Thời gian'}</th>
                  <th className="py-2.5 text-right">{isEn ? 'Actions' : 'Thao tác'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/80">
                {invoices.map((inv) => {
                  const meta = INVOICE_STATUS_META[inv.status] || INVOICE_STATUS_META.PENDING;
                  return (
                    <tr key={inv.id} className="text-slate-700 dark:text-slate-200">
                      <td className="py-3 pr-3 font-mono font-bold text-slate-900 dark:text-white">
                        {inv.invoiceCode}
                      </td>
                      <td className="py-3 pr-3 font-semibold">
                        {formatBillingCycleLabel(inv.billingCycle, language)}
                      </td>
                      <td className="py-3 pr-3 font-mono font-bold tabular-nums">
                        {fmtVND(inv.amount)}
                      </td>
                      <td className="py-3 pr-3">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-0.5 text-[10px] font-bold ${meta.cls}`}
                        >
                          {isEn ? meta.en : meta.vi}
                        </span>
                      </td>
                      <td className="py-3 pr-3 text-slate-500 dark:text-slate-400">
                        {formatDateTime(inv.paidAt || inv.createdAt, isEn)}
                      </td>
                      <td className="py-3 text-right">
                        {isAdmin && inv.status === 'PENDING' ? (
                          <div className="inline-flex items-center gap-1.5">
                            <button
                              type="button"
                              onClick={() => setActiveInvoice(inv)}
                              className="inline-flex h-8 items-center gap-1 rounded-lg border border-indigo-200 bg-indigo-50 px-2.5 text-[11px] font-bold text-indigo-700 hover:bg-indigo-600 hover:text-white dark:border-indigo-700/60 dark:bg-indigo-950/40 dark:text-indigo-300"
                            >
                              <QrCode size={12} />
                              <span>{isEn ? 'QR' : 'Mở QR'}</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => handleRowSync(inv.id)}
                              disabled={isSyncingRow}
                              className="inline-flex h-8 items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 text-[11px] font-bold text-slate-700 hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                            >
                              <RefreshCw size={12} />
                              <span>{isEn ? 'Sync' : 'Kiểm tra'}</span>
                            </button>
                          </div>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {liveActiveInvoice && (
        <SubscriptionQrModal
          invoice={liveActiveInvoice}
          onClose={() => setActiveInvoice(null)}
          onInvoiceUpdated={(updated) => {
            if (updated?.status === 'PAID') {
              setActiveInvoice(null);
            } else {
              setActiveInvoice(updated);
            }
          }}
        />
      )}
    </div>
  );
};

export default BillingManagement;
