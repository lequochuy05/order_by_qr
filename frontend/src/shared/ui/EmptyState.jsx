import React from 'react';

const EmptyState = ({ icon: Icon, message, className = '' }) => {
  return (
    <div
      className={`rounded-3xl border border-dashed border-slate-200 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-500 ${className}`}
    >
      {Icon && <Icon size={40} className="mx-auto mb-4 opacity-30" />}
      {message}
    </div>
  );
};

export default EmptyState;
