const SkeletonBlock = ({ className = '', style }) => (
  <div className={`skeleton-shimmer rounded-xl bg-slate-200 ${className}`} style={style} />
);

const CustomerMenuSkeleton = () => (
  <div className="flex h-dvh min-h-dvh justify-center overflow-hidden bg-gray-100">
    <div className="safe-left safe-right h-dvh w-full max-w-md overflow-y-auto bg-white shadow-2xl">
      <div
        className="rounded-b-[2.5rem] bg-orange-500 p-5 shadow-lg"
        style={{ paddingTop: 'calc(1.25rem + var(--safe-area-inset-top))' }}
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex flex-1 items-center gap-3">
            <SkeletonBlock className="h-10 w-10 shrink-0 bg-white/30" />
            <div className="flex-1 space-y-2">
              <SkeletonBlock className="h-5 w-36 bg-white/30" />
              <SkeletonBlock className="h-5 w-20 rounded-full bg-white/30" />
            </div>
          </div>
          <div className="flex gap-2">
            <SkeletonBlock className="h-8 w-8 rounded-full bg-white/30" />
            <SkeletonBlock className="h-8 w-16 rounded-full bg-white/30" />
          </div>
        </div>
      </div>

      <div className="space-y-5 p-4 pb-[calc(2rem+var(--safe-area-inset-bottom))]">
        <div className="flex gap-2 overflow-hidden border-b border-gray-100 py-4">
          {[80, 72, 112, 88].map((width) => (
            <SkeletonBlock key={width} className="h-9 shrink-0 rounded-full" style={{ width }} />
          ))}
        </div>

        <div className="grid grid-cols-2 gap-3">
          {Array.from({ length: 6 }, (_, index) => (
            <div
              key={index}
              className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm"
            >
              <SkeletonBlock className="aspect-square w-full rounded-none" />
              <div className="space-y-3 p-3">
                <SkeletonBlock className="h-4 w-4/5" />
                <SkeletonBlock className="h-3 w-full" />
                <SkeletonBlock className="h-3 w-2/3" />
                <div className="flex items-center justify-between pt-2">
                  <SkeletonBlock className="h-5 w-20" />
                  <SkeletonBlock className="h-8 w-8 rounded-full" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  </div>
);

export default CustomerMenuSkeleton;
