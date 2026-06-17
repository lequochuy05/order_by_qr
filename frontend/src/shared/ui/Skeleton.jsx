const SkeletonLine = ({ className = '' }) => (
  <div className={`animate-pulse bg-gray-200 rounded-lg dark:bg-slate-700 ${className}`} />
);

const SkeletonCard = ({ className = '' }) => (
  <div
    className={`animate-pulse bg-white rounded-2xl p-6 shadow-sm border border-gray-100 dark:bg-slate-900 dark:border-slate-800 ${className}`}
  >
    <div className="flex justify-between items-start mb-4">
      <div className="w-12 h-12 bg-gray-200 rounded-xl dark:bg-slate-700" />
      <div className="w-16 h-5 bg-gray-200 rounded-full dark:bg-slate-700" />
    </div>
    <div className="space-y-3">
      <div className="h-5 bg-gray-200 rounded-lg w-3/4 dark:bg-slate-700" />
      <div className="h-4 bg-gray-200 rounded-lg w-1/2 dark:bg-slate-700" />
    </div>
  </div>
);

const SkeletonTable = ({ rows = 5, cols = 4 }) => (
  <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden dark:bg-slate-900 dark:border-slate-800">
    {/* Header */}
    <div className="flex gap-4 p-5 border-b border-gray-100 dark:border-slate-800">
      {Array.from({ length: cols }).map((_, i) => (
        <div key={i} className="h-4 bg-gray-200 rounded flex-1 dark:bg-slate-700 animate-pulse" />
      ))}
    </div>
    {/* Rows */}
    {Array.from({ length: rows }).map((_, row) => (
      <div
        key={row}
        className="flex gap-4 p-5 border-b border-gray-50 last:border-0 dark:border-slate-800"
      >
        {Array.from({ length: cols }).map((_, col) => (
          <div
            key={col}
            className="h-4 bg-gray-100 rounded flex-1 dark:bg-slate-800 animate-pulse"
            style={{ animationDelay: `${(row * cols + col) * 50}ms` }}
          />
        ))}
      </div>
    ))}
  </div>
);

const SkeletonChart = ({ className = '' }) => (
  <div
    className={`animate-pulse bg-white rounded-2xl p-6 shadow-sm border border-gray-100 dark:bg-slate-900 dark:border-slate-800 ${className}`}
  >
    <div className="h-5 bg-gray-200 rounded-lg w-1/3 mb-6 dark:bg-slate-700" />
    <div className="flex items-end gap-2 h-48">
      {[40, 65, 45, 80, 55, 70, 50].map((h, i) => (
        <div
          key={i}
          className="flex-1 bg-gray-200 rounded-t-lg dark:bg-slate-700"
          style={{ height: `${h}%`, animationDelay: `${i * 100}ms` }}
        />
      ))}
    </div>
  </div>
);

export { SkeletonCard, SkeletonTable, SkeletonLine, SkeletonChart };
