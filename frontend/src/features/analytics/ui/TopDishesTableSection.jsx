import React from 'react';
import { Award, ChevronLeft, ChevronRight, UtensilsCrossed } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const RANK_COLORS = [
  'bg-gradient-to-r from-amber-400 to-amber-600 text-white shadow-md shadow-amber-500/25',
  'bg-gradient-to-r from-slate-400 to-slate-600 text-white shadow-md shadow-slate-500/25',
  'bg-gradient-to-r from-amber-700 to-amber-900 text-white shadow-md shadow-amber-800/25',
];

const TopDishesTableSection = ({
  dishes,
  paginatedDishes,
  maxQty,
  currentPage,
  totalPages,
  itemsPerPage,
  onPageChange,
}) => (
  <div className="rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] shadow-sm overflow-hidden">
    {/* Table Header */}
    <div className="p-6 border-b border-[#1E293B]/20 dark:border-[#1E293B] flex items-center justify-between">
      <div className="flex items-center gap-2.5">
        <span className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
          <Award size={20} />
        </span>
        <div>
          <h3 className="font-extrabold text-lg text-slate-900 dark:text-white tracking-tight">
            Bảng xếp hạng toàn bộ món ăn
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Danh sách tổng hợp sản lượng và doanh thu từng món
          </p>
        </div>
      </div>
      <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
        {dishes.length} món
      </span>
    </div>

    {/* Table View */}
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-950/50 text-slate-500 dark:text-slate-400 text-xs uppercase font-bold tracking-wider">
            <th className="p-4 w-16 text-center">#</th>
            <th className="p-4 w-16">Hình</th>
            <th className="p-4">Tên món</th>
            <th className="p-4">Danh mục</th>
            <th className="p-4 text-center">SL đã bán</th>
            <th className="p-4 text-right">Doanh thu</th>
            <th className="p-4 w-44 pr-6">Tỉ trọng bán</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
          {paginatedDishes.map((dish, idx) => {
            const rank = currentPage * itemsPerPage + idx + 1;
            const percentOfTop = Math.min(100, Math.round((dish.totalQty / maxQty) * 100));

            const rankBadge =
              rank <= 3 ? (
                <span
                  className={`inline-flex items-center justify-center w-8 h-8 rounded-xl text-xs font-black ${RANK_COLORS[rank - 1]}`}
                >
                  {rank}
                </span>
              ) : (
                <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 text-xs font-bold">
                  {rank}
                </span>
              );

            return (
              <tr
                key={`dish-${dish.menuItemId || rank}`}
                className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors"
              >
                <td className="p-4 text-center">{rankBadge}</td>

                <td className="p-4">
                  {dish.img ? (
                    <img
                      src={dish.img}
                      alt={dish.name}
                      className="w-11 h-11 rounded-xl object-cover border border-slate-200/80 dark:border-slate-700 shadow-sm"
                    />
                  ) : (
                    <div className="w-11 h-11 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200/60 dark:border-slate-700 flex items-center justify-center text-slate-400">
                      <UtensilsCrossed size={18} />
                    </div>
                  )}
                </td>

                <td className="p-4">
                  <p
                    className="font-bold text-slate-900 dark:text-white line-clamp-1"
                    title={dish.name}
                  >
                    {dish.name}
                  </p>
                </td>

                <td className="p-4">
                  <span className="inline-flex px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-400 border border-indigo-100/60 dark:border-indigo-900/40">
                    {dish.categoryName || dish.category || 'Khác'}
                  </span>
                </td>

                <td className="p-4 text-center">
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-black font-mono bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-400">
                    {dish.totalQty}
                  </span>
                </td>

                <td className="p-4 text-right font-black font-mono text-slate-900 dark:text-white">
                  {fmtVND(dish.totalRevenue)}
                </td>

                <td className="p-4 pr-6">
                  <div className="w-full flex items-center gap-2">
                    <div className="flex-1 bg-slate-100 dark:bg-slate-800 rounded-full h-2 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-700 ease-out"
                        style={{ width: `${percentOfTop}%` }}
                      />
                    </div>
                    <span className="text-[10px] font-bold text-slate-400 font-mono w-8 text-right">
                      {percentOfTop}%
                    </span>
                  </div>
                </td>
              </tr>
            );
          })}

          {dishes.length === 0 && (
            <tr key="empty-row">
              <td colSpan="7" className="p-12 text-center text-slate-400 dark:text-slate-500">
                <UtensilsCrossed className="w-12 h-12 mx-auto mb-3 opacity-20" />
                <p className="font-semibold text-sm">
                  Không có dữ liệu món ăn trong khoảng thời gian đã chọn.
                </p>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>

    {/* Pagination Controls */}
    {totalPages > 1 && (
      <div className="flex flex-col sm:flex-row justify-between items-center gap-3 p-4 px-6 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
        <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
          Hiển thị{' '}
          <span className="font-bold text-slate-800 dark:text-slate-200">
            {currentPage * itemsPerPage + 1}
          </span>{' '}
          -{' '}
          <span className="font-bold text-slate-800 dark:text-slate-200">
            {Math.min((currentPage + 1) * itemsPerPage, dishes.length)}
          </span>{' '}
          trong tổng số{' '}
          <span className="font-bold text-slate-800 dark:text-slate-200">{dishes.length}</span> món
        </span>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={currentPage === 0}
            onClick={() => onPageChange(Math.max(currentPage - 1, 0))}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/80 disabled:opacity-40 transition-all shadow-sm"
          >
            <ChevronLeft size={14} />
            <span>Trước</span>
          </button>

          <span className="px-3 py-1 rounded-lg text-xs font-bold text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
            {currentPage + 1} / {totalPages}
          </span>

          <button
            type="button"
            disabled={currentPage >= totalPages - 1}
            onClick={() => onPageChange(Math.min(currentPage + 1, totalPages - 1))}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/80 disabled:opacity-40 transition-all shadow-sm"
          >
            <span>Sau</span>
            <ChevronRight size={14} />
          </button>
        </div>
      </div>
    )}
  </div>
);

export default TopDishesTableSection;
