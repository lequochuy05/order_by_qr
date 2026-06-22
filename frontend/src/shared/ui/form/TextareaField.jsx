import FormError from '@shared/ui/FormError.jsx';
import FormLabel from './FormLabel.jsx';
import {
  FORM_CONTROL_CLASS,
  FORM_CONTROL_ERROR_CLASS,
  FORM_CONTROL_NORMAL_CLASS,
} from './formStyles.js';

const TextareaField = ({
  label,
  value,
  onChange,
  error,
  required = false,
  nativeRequired = false,
  maxLength,
  showCount = Boolean(maxLength),
  className = '',
  textareaClassName = '',
  rows = 3,
  ...props
}) => (
  <label className={`block ${className}`}>
    {label && <FormLabel required={required}>{label}</FormLabel>}
    <textarea
      rows={rows}
      maxLength={maxLength}
      value={value ?? ''}
      onChange={(event) => onChange?.(event.target.value, event)}
      required={nativeRequired}
      className={`${FORM_CONTROL_CLASS} resize-none ${
        error ? FORM_CONTROL_ERROR_CLASS : FORM_CONTROL_NORMAL_CLASS
      } ${textareaClassName}`}
      {...props}
    />
    <span className="flex items-start justify-between gap-3">
      <FormError message={error} />
      {showCount && (
        <span className="ml-auto mt-1.5 text-[10px] font-medium text-gray-400">
          {String(value ?? '').length}/{maxLength}
        </span>
      )}
    </span>
  </label>
);

export default TextareaField;
