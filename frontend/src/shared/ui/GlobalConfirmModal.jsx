import React from 'react';
import ConfirmModal from '@shared/ui/ConfirmModal.jsx';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';

const GlobalConfirmModal = () => {
  const { confirmModal, closeConfirm } = useConfirmModal();

  return (
    <ConfirmModal
      isOpen={confirmModal.isOpen}
      onClose={closeConfirm}
      onConfirm={confirmModal.onConfirm}
      title={confirmModal.title}
      message={confirmModal.message}
      loading={confirmModal.loading}
    />
  );
};

export default GlobalConfirmModal;
