import { createElement } from 'react';

import FormError from '@shared/ui/FormError.jsx';
import FormLabel from './FormLabel.jsx';
import {
  FORM_CONTROL_CLASS,
  FORM_CONTROL_ERROR_CLASS,
  FORM_CONTROL_NORMAL_CLASS,
} from './formStyles.js';

const TextField = ({
  label,
  value,
  onChange,
  type = 'text',
  error,
  required = false,
  nativeRequired = false,
  icon,
  prefix,
  suffix,
  className = '',
  inputClassName = '',
  ...props
}) => (
  <label className={`block ${className}`}>
    {label && <FormLabel required={required}>{label}</FormLabel>}
    <span className="relative block">
      {icon && (
        <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
          {createElement(icon, { size: 18 })}
        </span>
      )}
      {prefix && (
        <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-xs font-bold text-gray-400">
          {prefix}
        </span>
      )}
      <input
        type={type}
        value={value ?? ''}
        onChange={(event) => onChange?.(event.target.value, event)}
        required={nativeRequired}
        className={`${FORM_CONTROL_CLASS} ${
          error ? FORM_CONTROL_ERROR_CLASS : FORM_CONTROL_NORMAL_CLASS
        } ${icon || prefix ? 'pl-11' : ''} ${suffix ? 'pr-12' : ''} ${inputClassName}`}
        {...props}
      />
      {suffix && (
        <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-bold uppercase text-gray-400">
          {suffix}
        </span>
      )}
    </span>
    <FormError message={error} />
  </label>
);

export default TextField;
