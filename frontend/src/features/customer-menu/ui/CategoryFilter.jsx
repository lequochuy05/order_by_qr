const CategoryFilter = ({ categories, selectedCategory, onSelectCategory }) => {
  return (
    <div className="mb-4 border-b border-slate-200/80 bg-slate-50/90 py-3 backdrop-blur-md transition-colors duration-500 dark:border-slate-800/80 dark:bg-slate-950/90">
      <div className="flex gap-2 overflow-x-auto pb-1 px-1 scrollbar-hide">
        <button
          onClick={() => onSelectCategory('all')}
          className={`px-4 py-2 rounded-full whitespace-nowrap text-xs font-bold transition-all duration-300 active:scale-95 ${
            selectedCategory === 'all'
              ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
              : 'border border-slate-200/80 bg-white text-slate-600 hover:border-indigo-200 hover:bg-indigo-50/50 hover:text-indigo-600 dark:border-slate-800 dark:bg-slate-900/90 dark:text-slate-300 dark:hover:border-indigo-800'
          }`}
        >
          Tất cả
        </button>

        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => onSelectCategory(category.id)}
            className={`px-4 py-2 rounded-full whitespace-nowrap text-xs font-bold transition-all duration-300 active:scale-95 ${
              selectedCategory === category.id
                ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
                : 'border border-slate-200/80 bg-white text-slate-600 hover:border-indigo-200 hover:bg-indigo-50/50 hover:text-indigo-600 dark:border-slate-800 dark:bg-slate-900/90 dark:text-slate-300 dark:hover:border-indigo-800'
            }`}
          >
            {category.name}
          </button>
        ))}
      </div>
    </div>
  );
};

export default CategoryFilter;
