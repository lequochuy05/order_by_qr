const SettingsLayout = ({ title, subtitle, tabs, activeTabId, onTabChange, children }) => (
  <div className="grid w-full min-w-0 gap-4 sm:gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
    <aside className="min-w-0 space-y-3 rounded-2xl border border-[#E8DCD1] bg-white p-4 shadow-sm transition-colors dark:border-[#352C26] dark:bg-[#1F1A17]">
      <div className="px-1 py-2">
        <h1 className="text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">{title}</h1>
        <p className="mt-1 text-xs text-[#6B5D55] dark:text-[#BDB0A4]">{subtitle}</p>
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
              className={`flex min-w-0 w-full items-center gap-2 rounded-xl px-3 py-3 text-left text-xs font-bold transition sm:gap-3 sm:text-sm ${
                selected
                  ? 'bg-[#F7F0E7] text-[#56372B] shadow-sm dark:bg-[#2A221E] dark:text-[#D8986A]'
                  : 'text-[#6B5D55] hover:bg-[#F7F0E7]/60 hover:text-[#2D1D16] dark:text-[#BDB0A4] dark:hover:bg-[#2A221E]/60 dark:hover:text-[#F7F0E7]'
              }`}
            >
              <Icon className="shrink-0" size={18} />
              <span className="min-w-0 truncate">{tab.label}</span>
            </button>
          );
        })}
      </nav>
    </aside>

    <main className="min-w-0 rounded-2xl border border-[#E8DCD1] bg-white p-5 shadow-sm transition-colors sm:p-7 dark:border-[#352C26] dark:bg-[#1F1A17]">
      {children}
    </main>
  </div>
);

export default SettingsLayout;
