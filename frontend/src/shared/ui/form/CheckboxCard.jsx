const TONE_CLASSES = {
  orange: 'text-orange-500 focus:ring-orange-200',
  green: 'text-emerald-500 focus:ring-emerald-200',
  red: 'text-red-500 focus:ring-red-200',
};

const CheckboxCard = ({
  checked,
  onChange,
  label,
  description,
  tone = 'orange',
  disabled = false,
  className = '',
}) => (
  <label
    className={`flex min-h-[52px] w-full cursor-pointer items-center gap-3 rounded-2xl border-2 border-transparent bg-gray-50 px-5 transition-all hover:bg-gray-100 ${
      disabled ? 'cursor-not-allowed opacity-60' : ''
    } ${className}`}
  >
    <input
      type="checkbox"
      className={`h-5 w-5 cursor-pointer rounded-md border-gray-200 ${
        TONE_CLASSES[tone] || TONE_CLASSES.orange
      }`}
      checked={Boolean(checked)}
      onChange={(event) => onChange?.(event.target.checked, event)}
      disabled={disabled}
    />
    <span className="min-w-0">
      <span className="block text-xs font-black uppercase tracking-tight text-gray-600">
        {label}
      </span>
      {description && (
        <span className="mt-0.5 block text-[11px] font-medium text-gray-400">{description}</span>
      )}
    </span>
  </label>
);

export default CheckboxCard;
