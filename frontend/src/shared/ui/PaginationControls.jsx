import { ChevronLeft, ChevronRight } from 'lucide-react';

const PaginationControls = ({
  currentPage = 0,
  totalPages = 0,
  totalElements = 0,
  itemLabel = 'mục',
  loading = false,
  onPageChange
}) => {
  if (totalPages <= 1) return null;

  const goToPage = (page) => {
    if (loading || page < 0 || page >= totalPages || page === currentPage) return;
    onPageChange?.(page);
  };

  return (
    <div className="flex items-center justify-center gap-3 pt-2">
      <button
        type="button"
        disabled={loading || currentPage === 0}
        onClick={() => goToPage(currentPage - 1)}
        className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <ChevronLeft size={16} /> Trước
      </button>
      <span className="px-4 text-sm font-bold text-gray-500">
        Trang {currentPage + 1} / {totalPages}
        <span className="ml-2 font-medium text-gray-400">({totalElements} {itemLabel})</span>
      </span>
      <button
        type="button"
        disabled={loading || currentPage >= totalPages - 1}
        onClick={() => goToPage(currentPage + 1)}
        className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Sau <ChevronRight size={16} />
      </button>
    </div>
  );
};

export default PaginationControls;
