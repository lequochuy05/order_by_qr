import { ChevronLeft, ChevronRight } from 'lucide-react';

const PaginationControls = ({
  currentPage = 0,
  totalPages = 0,
  totalElements = 0,
  itemLabel = 'mục',
  loading = false,
  onPageChange,
}) => {
  if (totalPages <= 1) return null;

  const goToPage = (page) => {
    if (loading || page < 0 || page >= totalPages || page === currentPage) return;
    onPageChange?.(page);
  };

  return (
    <div className="flex min-w-0 flex-wrap items-center justify-center gap-2 pt-2 sm:gap-3">
      <button
        type="button"
        disabled={loading || currentPage === 0}
        onClick={() => goToPage(currentPage - 1)}
        className="inline-flex items-center gap-1.5 rounded-xl border border-[#1E293B]/25 bg-white/90 backdrop-blur-sm px-3.5 py-2 text-xs font-bold text-slate-700 shadow-sm transition-all hover:border-indigo-400 hover:bg-indigo-50/50 hover:text-indigo-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-[#1E293B]/25 disabled:hover:bg-white/90 disabled:hover:text-slate-700 dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-300 dark:hover:border-indigo-500 dark:hover:bg-indigo-950/30 dark:hover:text-indigo-400 sm:px-4"
      >
        <ChevronLeft size={15} /> Trước
      </button>
      <span className="order-first w-full px-2 text-center text-xs font-bold text-slate-600 dark:text-slate-300 sm:order-none sm:w-auto sm:px-4">
        Trang <span className="font-mono text-indigo-600 dark:text-indigo-400">{currentPage + 1}</span> / {totalPages}
        <span className="ml-2 font-medium text-slate-400 dark:text-slate-500">
          ({totalElements} {itemLabel})
        </span>
      </span>
      <button
        type="button"
        disabled={loading || currentPage >= totalPages - 1}
        onClick={() => goToPage(currentPage + 1)}
        className="inline-flex items-center gap-1.5 rounded-xl border border-[#1E293B]/25 bg-white/90 backdrop-blur-sm px-3.5 py-2 text-xs font-bold text-slate-700 shadow-sm transition-all hover:border-indigo-400 hover:bg-indigo-50/50 hover:text-indigo-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-[#1E293B]/25 disabled:hover:bg-white/90 disabled:hover:text-slate-700 dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-300 dark:hover:border-indigo-500 dark:hover:bg-indigo-950/30 dark:hover:text-indigo-400 sm:px-4"
      >
        Sau <ChevronRight size={15} />
      </button>
    </div>
  );
};

export default PaginationControls;
