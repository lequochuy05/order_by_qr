import { create } from 'zustand';

const closedState = {
  isOpen: false,
  title: '',
  message: '',
  onConfirm: null,
  loading: false,
  resolveConfirm: null,
};

export const useConfirmModalStore = create((set, get) => ({
  isOpen: false,
  title: '',
  message: '',
  onConfirm: null,
  loading: false,
  resolveConfirm: null,

  confirm: (title, message) => {
    return new Promise((resolve) => {
      get().resolveConfirm?.(false);
      set({
        isOpen: true,
        title,
        message,
        loading: false,
        resolveConfirm: resolve,
        onConfirm: () => {
          const resolveConfirm = get().resolveConfirm;
          set(closedState);
          resolveConfirm?.(true);
        },
      });
    });
  },

  closeConfirm: () => {
    const resolveConfirm = get().resolveConfirm;
    set(closedState);
    resolveConfirm?.(false);
  },
}));

export const useConfirmModal = () => {
  const store = useConfirmModalStore();

  return {
    confirmModal: {
      isOpen: store.isOpen,
      title: store.title,
      message: store.message,
      onConfirm: store.onConfirm,
      loading: store.loading,
    },
    confirm: store.confirm,
    closeConfirm: store.closeConfirm,
  };
};
