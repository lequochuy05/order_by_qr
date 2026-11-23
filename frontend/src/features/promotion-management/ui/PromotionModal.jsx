import React, { useMemo, useState } from 'react';
import { CalendarDays, Clock3, Percent } from 'lucide-react';
import {
  DAYS_OF_WEEK_ORDER,
  WEEKDAY_CODES,
  WEEKEND_CODES,
  formatPromotionTime,
  isEveryDayPromotion,
  parseTimeToMinutes,
  sortDaysOfWeek,
  toApiLocalTime,
} from '@entities/promotion';
import { useTranslation } from '@shared/lib/i18n';
import {
  CheckboxCard,
  FormError,
  FormLabel,
  ModalActions,
  ModalHeader,
  SharedModal,
  TextField,
} from '@shared/ui';

const normalizeComparableDays = (days) => {
  const sorted = sortDaysOfWeek(days);
  return sorted.length === DAYS_OF_WEEK_ORDER.length ? [] : sorted;
};

const PromotionModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData,
  isSubmitting,
  serverErrors = {},
  onClearServerError,
}) => {
  const { isEn, copy } = useTranslation('promotions');

  const [formData, setFormData] = useState(() => {
    if (initialData) {
      return {
        name: initialData.name || '',
        discountPercent:
          initialData.discountPercent !== null && initialData.discountPercent !== undefined
            ? String(initialData.discountPercent)
            : '',
        startTime: formatPromotionTime(initialData.startTime),
        endTime: formatPromotionTime(initialData.endTime),
        daysOfWeek: sortDaysOfWeek(initialData.daysOfWeek),
        active: initialData.active ?? true,
      };
    }

    return {
      name: '',
      discountPercent: '',
      startTime: '11:00',
      endTime: '14:00',
      daysOfWeek: [],
      active: true,
    };
  });

  const [errors, setErrors] = useState({});

  const originalComparable = useMemo(() => {
    if (!initialData?.id) return null;
    return {
      name: initialData.name || '',
      discountPercent:
        initialData.discountPercent !== null && initialData.discountPercent !== undefined
          ? String(initialData.discountPercent)
          : '',
      startTime: formatPromotionTime(initialData.startTime),
      endTime: formatPromotionTime(initialData.endTime),
      daysOfWeek: normalizeComparableDays(initialData.daysOfWeek),
      active: initialData.active ?? true,
    };
  }, [initialData]);

  const isChanged = useMemo(() => {
    if (!initialData?.id) return true;
    return (
      JSON.stringify(originalComparable) !==
      JSON.stringify({
        name: formData.name,
        discountPercent: formData.discountPercent,
        startTime: formatPromotionTime(formData.startTime),
        endTime: formatPromotionTime(formData.endTime),
        daysOfWeek: normalizeComparableDays(formData.daysOfWeek),
        active: formData.active,
      })
    );
  }, [formData, initialData, originalComparable]);

  const toggleDay = (day) => {
    setFormData((prev) => {
      const exists = prev.daysOfWeek.includes(day);
      const nextDays = exists
        ? prev.daysOfWeek.filter((d) => d !== day)
        : [...prev.daysOfWeek, day];
      return { ...prev, daysOfWeek: sortDaysOfWeek(nextDays) };
    });
    if (errors.daysOfWeek) setErrors((prev) => ({ ...prev, daysOfWeek: null }));
    onClearServerError?.('daysOfWeek');
  };

  const applyPreset = (days) => {
    setFormData((prev) => ({ ...prev, daysOfWeek: sortDaysOfWeek(days) }));
    if (errors.daysOfWeek) setErrors((prev) => ({ ...prev, daysOfWeek: null }));
    onClearServerError?.('daysOfWeek');
  };

  const validateForm = () => {
    const newErrors = {};
    const trimmedName = formData.name.trim();

    if (!trimmedName) {
      newErrors.name = copy.errNameRequired;
    } else if (trimmedName.length > 150) {
      newErrors.name = copy.errNameMax;
    }

    const percent = parseFloat(formData.discountPercent);
    if (Number.isNaN(percent) || percent <= 0 || percent > 100) {
      newErrors.discountPercent = copy.errDiscountRange;
    }

    const startMinutes = parseTimeToMinutes(formData.startTime);
    const endMinutes = parseTimeToMinutes(formData.endTime);

    if (startMinutes === null) {
      newErrors.startTime = copy.errStartTimeRequired;
    }
    if (endMinutes === null) {
      newErrors.endTime = copy.errEndTimeRequired;
    }
    if (startMinutes !== null && endMinutes !== null && endMinutes <= startMinutes) {
      newErrors.endTime = copy.errTimeRange;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    const normalizedDays = sortDaysOfWeek(formData.daysOfWeek);

    onSubmit({
      name: formData.name.trim(),
      discountPercent: parseFloat(formData.discountPercent),
      startTime: toApiLocalTime(formData.startTime),
      endTime: toApiLocalTime(formData.endTime),
      daysOfWeek: normalizedDays.length === DAYS_OF_WEEK_ORDER.length ? [] : normalizedDays,
      active: Boolean(formData.active),
    });
  };

  if (!isOpen) return null;

  const isEveryDaySelected = isEveryDayPromotion(formData.daysOfWeek);
  const isWeekdaysSelected =
    formData.daysOfWeek.length === WEEKDAY_CODES.length &&
    WEEKDAY_CODES.every((d) => formData.daysOfWeek.includes(d));
  const isWeekendSelected =
    formData.daysOfWeek.length === WEEKEND_CODES.length &&
    WEEKEND_CODES.every((d) => formData.daysOfWeek.includes(d));

  const nameError = errors.name || serverErrors.name;
  const discountError = errors.discountPercent || serverErrors.discountPercent;
  const startTimeError = errors.startTime || serverErrors.startTime;
  const endTimeError = errors.endTime || serverErrors.endTime || serverErrors.validTimeRange;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-lg !p-0"
      ariaLabel={initialData?.id ? copy.modalEditTitle : copy.modalAddTitle}
    >
      <ModalHeader
        title={initialData?.id ? copy.modalEditTitle : copy.modalAddTitle}
        subtitle={copy.modalSubtitle}
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="promotionForm"
        onSubmit={handleSubmit}
        className="custom-scrollbar flex-1 space-y-6 overflow-y-auto p-6 sm:p-8"
      >
        <TextField
          label={copy.nameLabel}
          required
          maxLength={150}
          placeholder={copy.namePlaceholder}
          value={formData.name}
          onChange={(value) => {
            setFormData((prev) => ({ ...prev, name: value }));
            if (errors.name) setErrors((prev) => ({ ...prev, name: null }));
            onClearServerError?.('name');
          }}
          error={nameError}
        />

        <TextField
          label={copy.discountPercentLabel}
          required
          type="number"
          step="0.01"
          min="0.01"
          max="100"
          icon={Percent}
          suffix="%"
          placeholder={copy.discountPercentPlaceholder}
          value={formData.discountPercent}
          onChange={(value) => {
            setFormData((prev) => ({ ...prev, discountPercent: value }));
            if (errors.discountPercent) setErrors((prev) => ({ ...prev, discountPercent: null }));
            onClearServerError?.('discountPercent');
          }}
          error={discountError}
        />

        <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-5 dark:border-slate-800/80 dark:bg-slate-800/40">
          <p className="mb-4 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            <Clock3 size={13} />
            {copy.timeSlot}
          </p>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <TextField
              label={copy.startTimeLabel}
              required
              type="time"
              value={formData.startTime}
              onChange={(value) => {
                setFormData((prev) => ({ ...prev, startTime: value }));
                if (errors.startTime || errors.endTime) {
                  setErrors((prev) => ({ ...prev, startTime: null, endTime: null }));
                }
                onClearServerError?.('startTime');
                onClearServerError?.('validTimeRange');
              }}
              error={startTimeError}
            />
            <TextField
              label={copy.endTimeLabel}
              required
              type="time"
              value={formData.endTime}
              onChange={(value) => {
                setFormData((prev) => ({ ...prev, endTime: value }));
                if (errors.endTime) setErrors((prev) => ({ ...prev, endTime: null }));
                onClearServerError?.('endTime');
                onClearServerError?.('validTimeRange');
              }}
              error={endTimeError}
            />
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-5 dark:border-slate-800/80 dark:bg-slate-800/40">
          <div className="mb-3 flex items-center justify-between gap-2">
            <FormLabel className="mb-0 flex items-center gap-1.5">
              <CalendarDays size={13} />
              {copy.daysOfWeekLabel}
            </FormLabel>
          </div>

          <div className="mb-3 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => applyPreset([])}
              className={`rounded-xl px-3 py-1.5 text-[11px] font-bold transition-all ${
                isEveryDaySelected
                  ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/25'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800'
              }`}
            >
              {copy.everyDay}
            </button>
            <button
              type="button"
              onClick={() => applyPreset(WEEKDAY_CODES)}
              className={`rounded-xl px-3 py-1.5 text-[11px] font-bold transition-all ${
                isWeekdaysSelected
                  ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/25'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800'
              }`}
            >
              {copy.weekdays}
            </button>
            <button
              type="button"
              onClick={() => applyPreset(WEEKEND_CODES)}
              className={`rounded-xl px-3 py-1.5 text-[11px] font-bold transition-all ${
                isWeekendSelected
                  ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/25'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800'
              }`}
            >
              {copy.weekend}
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1.5">
            {DAYS_OF_WEEK_ORDER.map((day) => {
              const selected = formData.daysOfWeek.includes(day);
              return (
                <button
                  key={day}
                  type="button"
                  onClick={() => toggleDay(day)}
                  title={copy[`dayFull${day}`]}
                  aria-pressed={selected}
                  className={`rounded-xl py-2.5 text-xs font-black transition-all active:scale-95 ${
                    selected
                      ? 'bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/25'
                      : 'border border-slate-200/80 bg-white text-slate-600 hover:border-indigo-300 hover:text-indigo-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:border-indigo-500/50 dark:hover:text-indigo-400'
                  }`}
                >
                  {copy[`dayShort${day}`]}
                </button>
              );
            })}
          </div>

          <p className="mt-3 text-[11px] font-medium text-slate-400 dark:text-slate-500">
            {copy.everyDayHint}
          </p>
          <FormError message={errors.daysOfWeek || serverErrors.daysOfWeek} />
        </div>

        <CheckboxCard
          checked={formData.active}
          onChange={(checked) => setFormData((prev) => ({ ...prev, active: checked }))}
          label={copy.activeLabel}
          description={copy.activeDescription}
        />
      </form>

      <ModalActions
        onClose={onClose}
        formId="promotionForm"
        submitLabel={initialData?.id ? copy.submitUpdate : copy.submitCreate}
        submittingLabel={isEn ? 'Saving...' : 'Đang lưu...'}
        cancelLabel={isEn ? 'Cancel' : 'Hủy bỏ'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default PromotionModal;
