import React from 'react';
import StatusModal from '@shared/ui/StatusModal.jsx';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';

const GlobalStatusModal = () => {
  const { statusModal, closeStatusModal } = useStatusModal();

  return (
    <StatusModal
      isOpen={statusModal.isOpen}
      onClose={closeStatusModal}
      type={statusModal.type}
      title={statusModal.title}
      message={statusModal.message}
    />
  );
};

export default GlobalStatusModal;
