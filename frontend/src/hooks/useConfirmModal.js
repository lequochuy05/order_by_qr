import { useState, useCallback } from 'react';

export const useConfirmModal = () => {
  const [state, setState] = useState({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: null,
    loading: false,
  });

  const confirm = useCallback((title, message) => {
    return new Promise((resolve) => {
      setState({
        isOpen: true,
        title,
        message,
        loading: false,
        onConfirm: () => {
          setState(prev => ({ ...prev, isOpen: false }));
          resolve(true);
        },
      });
    });
  }, []);

  const closeConfirm = useCallback(() => {
    setState(prev => ({ ...prev, isOpen: false }));
  }, []);

  return {
    confirmModal: state,
    confirm,
    closeConfirm,
  };
};
