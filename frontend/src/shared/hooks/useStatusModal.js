import { create } from 'zustand';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';

export const useStatusModalStore = create((set) => ({
  isOpen: false,
  type: 'success',
  title: '',
  message: '',

  showSuccess: (msg, title = 'Thành công!') =>
    set({
      isOpen: true,
      type: 'success',
      title: title,
      message: msg,
    }),

  showError: (err, title = 'Thao tác thất bại') =>
    set({
      isOpen: true,
      type: 'error',
      title: title,
      message: buildErrorMessage(err),
    }),

  closeStatusModal: () => set({ isOpen: false }),
}));

export const useStatusModal = () => {
  const store = useStatusModalStore();

  return {
    statusModal: {
      isOpen: store.isOpen,
      type: store.type,
      title: store.title,
      message: store.message,
    },
    showSuccess: store.showSuccess,
    showError: store.showError,
    closeStatusModal: store.closeStatusModal,
  };
};
