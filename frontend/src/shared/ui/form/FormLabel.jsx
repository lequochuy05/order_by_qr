import { FORM_LABEL_CLASS } from './formStyles.js';

const FormLabel = ({ children, required = false, className = '' }) => (
  <span className={`${FORM_LABEL_CLASS} ${className}`}>
    {children} {required && <span className="text-red-500">*</span>}
  </span>
);

export default FormLabel;
