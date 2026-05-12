import React from 'react';

const AdminFooter = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="h-16 bg-white border-t border-gray-100 px-8 flex flex-col md:flex-row items-center justify-between text-sm text-gray-500 shadow-[0_-1px_3px_rgba(0,0,0,0.02)]">

      <div className="flex items-center gap-1 font-medium">
        <span>© {currentYear}</span>
        <span className="text-gray-800 font-bold"> - WucHuy</span>
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