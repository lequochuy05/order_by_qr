import { useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, Crown, Loader2, Lock, Sparkles } from 'lucide-react';

import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { ModalHeader, SharedModal } from '@shared/ui';
import { useActivateTrialMutation } from '../api/subscriptionMutations.js';
import { getFeaturePaywallCopy } from '../lib/subscriptionHelpers.js';
import { useSubscription } from '../model/useSubscription.js';

const PRO_HIGHLIGHTS = [
  {
    vi: 'Trọn bộ Trợ lý AI: Khách đặt món 1 chạm, Tóm tắt ca, Quét hóa đơn Vision, Dự báo kho BOM & Đề xuất Combo',
    en: 'Full AI Suite: 1-tap AI ordering, Executive summaries, Vision invoice OCR, BOM restock & Combo suggestions',
  },
  {
    vi: 'Quản trị Kho nguyên liệu & Công thức định lượng (BOM) theo thời gian thực',
    en: 'Full Inventory & Dish BOM recipe management with live Food Cost % tracking',
  },
  {
    vi: 'Không giới hạn số lượng bàn ăn & tài khoản nhân sự vận hành',
    en: 'Unlimited dining tables and internal staff accounts',
  },
];

const UpgradeProModal = ({
  isOpen,
  onClose,
  feature,
  isAdmin = false,
  canViewBilling = false,
}) => {
  const navigate = useNavigate();
  const { language, isEn } = useTranslation();
  const { trialUsed } = useSubscription({ enabled: Boolean(isOpen) });
  const { mutateAsync: activateTrial, isPending: isActivatingTrial } = useActivateTrialMutation();

  if (!isOpen) return null;

  const copy = getFeaturePaywallCopy(feature, language);

  const handleStartTrial = async () => {
    if (!isAdmin || isActivatingTrial) return;
    try {
      await activateTrial();
      showSuccessToast(
        isEn
          ? '14-day QROS Pro trial activated! All Pro features are now unlocked.'
          : 'Đã kích hoạt dùng thử QROS Pro 14 ngày! Toàn bộ tính năng Pro đã được mở khóa.',
      );
      onClose?.();
    } catch (error) {
      showErrorToast(error);
    }
  };

  const handleGoBilling = () => {
    onClose?.();
    navigate('/admin/billing');
  };

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={() => {
        if (!isActivatingTrial) onClose?.();
      }}
      className="max-w-lg !p-0"
      ariaLabel={copy.title}
    >
      <ModalHeader
        title={copy.title}
        subtitle={isEn ? 'QROS Pro Subscription' : 'Gói dịch vụ QROS Pro'}
        onClose={() => {
          if (!isActivatingTrial) onClose?.();
        }}
        disabled={isActivatingTrial}
      />

      <div className="space-y-5 p-6">
        <div className="flex items-start gap-3.5 rounded-2xl border border-amber-200/80 bg-amber-50/70 p-4 dark:border-amber-500/30 dark:bg-amber-950/30">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-amber-500 text-white shadow-sm">
            <Crown size={20} />
          </div>
          <p className="text-xs leading-relaxed font-medium text-amber-950 dark:text-amber-200">
            {copy.description}
          </p>
        </div>

        <div className="space-y-2.5">
          <p className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400">
            {isEn ? 'Included in QROS Pro' : 'Quyền lợi mở khóa trong gói Pro'}
          </p>
          <ul className="space-y-2">
            {PRO_HIGHLIGHTS.map((item, idx) => (
              <li
                key={idx}
                className="flex items-start gap-2.5 text-xs font-medium text-slate-700 dark:text-slate-200"
              >
                <CheckCircle2 size={15} className="mt-0.5 shrink-0 text-emerald-500" />
                <span>{isEn ? item.en : item.vi}</span>
              </li>
            ))}
          </ul>
        </div>

        {!isAdmin && (
          <div className="flex items-start gap-2.5 rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
            <Lock size={14} className="mt-0.5 shrink-0 text-slate-400" />
            <span>
              {isEn
                ? 'Only the Restaurant Owner (Admin) can activate the free trial or upgrade the subscription plan.'
                : 'Chỉ Chủ nhà hàng (Admin) mới có quyền kích hoạt dùng thử hoặc thanh toán nâng cấp gói Pro.'}
            </span>
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-end gap-2.5 border-t border-slate-200/80 bg-slate-50/80 px-6 py-4 dark:border-slate-800 dark:bg-slate-900/80">
        <button
          type="button"
          onClick={onClose}
          disabled={isActivatingTrial}
          className="inline-flex h-10 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          {isEn ? 'Later' : 'Để sau'}
        </button>

        {isAdmin && !trialUsed && (
          <button
            type="button"
            onClick={handleStartTrial}
            disabled={isActivatingTrial}
            className="inline-flex h-10 items-center gap-2 rounded-xl border border-indigo-200 bg-indigo-50 px-4 text-xs font-bold text-indigo-700 transition hover:bg-indigo-600 hover:text-white disabled:opacity-50 dark:border-indigo-700/60 dark:bg-indigo-950/50 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
          >
            {isActivatingTrial ? (
              <Loader2 size={14} className="animate-spin" />
            ) : (
              <Sparkles size={14} />
            )}
            <span>{isEn ? 'Start 14-Day Free Trial' : 'Dùng thử 14 ngày miễn phí'}</span>
          </button>
        )}

        {(isAdmin || canViewBilling) && (
          <button
            type="button"
            onClick={handleGoBilling}
            disabled={isActivatingTrial}
            className="inline-flex h-10 items-center gap-2 rounded-xl bg-indigo-600 px-4 text-xs font-bold text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-50"
          >
            <span>{isEn ? 'View Plans & Upgrade' : 'Xem gói & Nâng cấp'}</span>
            <ArrowRight size={14} />
          </button>
        )}
      </div>
    </SharedModal>
  );
};

export default UpgradeProModal;
