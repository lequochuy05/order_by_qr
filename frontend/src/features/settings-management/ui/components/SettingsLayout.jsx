const SettingsLayout = ({ title, subtitle, tabs, activeTabId, onTabChange, children }) => (
  <div className="grid w-full min-w-0 gap-4 sm:gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
    <aside className="min-w-0 space-y-3 rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-900">
      <div className="px-1 py-2">
        <h1 className="text-lg font-black text-slate-900 dark:text-slate-100">{title}</h1>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{subtitle}</p>
      </div>
      <nav className="grid grid-cols-2 gap-1 sm:grid-cols-4 lg:grid-cols-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const selected = activeTabId === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => onTabChange(tab.id)}
              className={`flex min-w-0 w-full items-center gap-2 rounded-lg px-2 py-3 text-left text-xs font-bold transition sm:gap-3 sm:px-3 sm:text-sm ${
                selected
                  ? 'bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'
              }`}
            >
              <Icon className="shrink-0" size={18} />
              <span className="min-w-0 truncate">{tab.label}</span>
            </button>
          );
        })}
      </nav>
    </aside>

    <main className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
      {children}
    </main>
  </div>
);

export default SettingsLayout;
