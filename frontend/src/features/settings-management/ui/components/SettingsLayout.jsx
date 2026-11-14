const SettingsLayout = ({ title, subtitle, tabs, activeTabId, onTabChange, children }) => (
  <div className="grid w-full min-w-0 gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
    <aside className="min-w-0 space-y-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90">
      <div className="px-2 py-1">
        <h1 className="text-xl font-black tracking-tight text-slate-900 dark:text-white">{title}</h1>
        <p className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">{subtitle}</p>
      </div>
      <nav className="grid grid-cols-2 gap-1.5 sm:grid-cols-4 lg:grid-cols-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const selected = activeTabId === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => onTabChange(tab.id)}
              className={`flex min-w-0 w-full items-center gap-2.5 rounded-2xl px-3.5 py-3 text-left text-xs font-bold transition-all sm:text-sm ${
                selected
                  ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
                  : 'text-slate-600 hover:bg-slate-100/80 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800/80 dark:hover:text-white'
              }`}
            >
              <Icon className="shrink-0" size={18} />
              <span className="min-w-0 truncate">{tab.label}</span>
            </button>
          );
        })}
      </nav>
    </aside>

    <main className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-colors sm:p-8 dark:border-[#1E293B] dark:bg-[#0F172A]/90">
      {children}
    </main>
  </div>
);

export default SettingsLayout;
