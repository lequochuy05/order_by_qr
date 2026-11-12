import React from 'react';

const AdminFooter = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="flex min-h-14 w-full min-w-0 flex-col items-center justify-between border-t border-[#E8DCD1] bg-white px-4 py-3 text-xs text-[#6B5D55] transition-colors sm:px-8 md:flex-row dark:border-[#352C26] dark:bg-[#1F1A17] dark:text-[#BDB0A4]">
      <div className="flex items-center gap-1 font-medium">
        <span>© {currentYear}</span>
        <span className="font-bold text-[#2D1D16] dark:text-[#F7F0E7]">wuchuy05</span>
      </div>
      <div className="text-[11px] text-[#9C8E85] dark:text-[#82756B]">QROS</div>
    </footer>
  );
};

export default AdminFooter;
