import React from 'react';

const AdminFooter = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="flex min-h-16 w-full min-w-0 flex-col items-center justify-between border-t border-gray-100 bg-white px-3 py-3 text-sm text-gray-500 shadow-[0_-1px_3px_rgba(0,0,0,0.02)] transition-colors sm:px-8 md:flex-row dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
      <div className="flex items-center gap-1 font-medium">
        <span>© {currentYear}</span>
        <span className="text-gray-800 font-bold dark:text-slate-100"> - WucHuy</span>
      </div>

      {/* <div className="flex items-center gap-6">

        <div className="hidden md:flex items-center gap-2">
          <a href="#"><img src="https://img.shields.io/badge/GitHub-121013?style=for-the-badge&logo=github&logoColor=white" alt="GitHub" /></a>
        </div>
      </div> */}
    </footer>
  );
};

export default AdminFooter;
