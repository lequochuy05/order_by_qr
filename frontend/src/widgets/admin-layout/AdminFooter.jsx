import React from 'react';

const AdminFooter = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="flex min-h-14 w-full min-w-0 flex-col items-center justify-between border-t border-[#1E293B]/20 bg-white/80 backdrop-blur-md px-4 sm:px-8 py-3 text-xs text-slate-500 transition-colors md:flex-row dark:border-[#1E293B] dark:bg-[#0F172A]/80 dark:text-slate-400">
      <div className="flex items-center gap-1.5 font-medium">
        <span>© {currentYear}</span>
        <span className="font-extrabold text-slate-800 dark:text-slate-200">QROS</span>
        <span className="text-slate-400 dark:text-slate-600">•</span>
        <span>Hệ thống Quản lý Nhà hàng & Gọi món QR</span>
      </div>
      <div className="flex items-center gap-2 text-[11px] text-slate-400 dark:text-slate-500">
        <span className="inline-flex items-center gap-1">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          Hệ thống ổn định
        </span>
        <span>•</span>
        <span className="font-mono font-semibold">v2.1</span>
      </div>
    </footer>
  );
};

export default AdminFooter;
