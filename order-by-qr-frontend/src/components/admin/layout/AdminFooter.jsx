import React from 'react';

const AdminFooter = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="h-16 bg-white border-t border-gray-100 px-8 flex flex-col md:flex-row items-center justify-between text-sm text-gray-500 shadow-[0_-1px_3px_rgba(0,0,0,0.02)]">
   
      <div className="flex items-center gap-1 font-medium">
        <span>© {currentYear}</span>
        <span className="text-gray-800 font-bold"> - Lê Quốc Huy</span>
      </div>

      <div className="flex items-center gap-6">
        {/* Phiên bản hệ thống */}
        <div className="hidden md:flex items-center gap-2">
          <span className="px-2 py-0.5 bg-gray-100 text-gray-400 text-[10px] font-bold rounded-full border border-gray-200 uppercase tracking-widest">
            v0.1
          </span>
        </div>
      </div>
    </footer>
  );
};

export default AdminFooter;