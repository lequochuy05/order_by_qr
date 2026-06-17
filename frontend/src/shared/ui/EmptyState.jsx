import React from 'react';

const EmptyState = ({ icon: Icon, message, className = '' }) => {
  return (
    <div
      className={`text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed ${className}`}
    >
      {Icon && <Icon size={40} className="mx-auto mb-4 opacity-30" />}
      {message}
    </div>
  );
};

export default EmptyState;
