import { createElement } from 'react';

import FormError from '@shared/ui/FormError.jsx';
import FormLabel from './FormLabel.jsx';
import {
  FORM_CONTROL_CLASS,
  FORM_CONTROL_ERROR_CLASS,
  FORM_CONTROL_NORMAL_CLASS,
} from './formStyles.js';

const SelectField = ({
  label,
  value,
  onChange,
  error,
  required = false,
  nativeRequired = false,
  icon,
  options,
  children,
  className = '',
  selectClassName = '',
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
      <select
        value={value ?? ''}
        onChange={(event) => onChange?.(event.target.value, event)}
        required={nativeRequired}
        className={`${FORM_CONTROL_CLASS} cursor-pointer appearance-none ${
          error ? FORM_CONTROL_ERROR_CLASS : FORM_CONTROL_NORMAL_CLASS
        } ${icon ? 'pl-11' : ''} ${selectClassName}`}
        {...props}
      >
        {children ||
          options?.map((option) => (
            <option
              key={option.value ?? option.id}
              value={option.value ?? option.id}
              disabled={option.disabled}
            >
              {option.label ?? option.name}
            </option>
          ))}
      </select>
    </span>
    <FormError message={error} />
  </label>
);

export default SelectField;
