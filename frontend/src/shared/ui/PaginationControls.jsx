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
        className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 sm:px-4"
      >
        <ChevronLeft size={16} /> Trước
      </button>
      <span className="order-first w-full px-2 text-center text-sm font-bold text-gray-500 sm:order-none sm:w-auto sm:px-4">
        Trang {currentPage + 1} / {totalPages}
        <span className="ml-2 font-medium text-gray-400">
          ({totalElements} {itemLabel})
        </span>
      </span>
      <button
        type="button"
        disabled={loading || currentPage >= totalPages - 1}
        onClick={() => goToPage(currentPage + 1)}
        className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 sm:px-4"
      >
        Sau <ChevronRight size={16} />
      </button>
    </div>
  );
};

export default PaginationControls;
