import toast from 'react-hot-toast';
import { buildErrorMessage } from './errorMessages.js';

export const showSuccessToast = (message, options = {}) =>
  toast.success(message, {
    duration: 3000,
    ...options,
  });

export const showErrorToast = (error, options = {}) =>
  toast.error(buildErrorMessage(error, { includeDetails: false }), {
    duration: 5000,
    ...options,
  });
