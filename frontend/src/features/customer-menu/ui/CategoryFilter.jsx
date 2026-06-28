const CategoryFilter = ({ categories, selectedCategory, onSelectCategory }) => {
  return (
    <div className="mb-4 border-b border-gray-100 bg-white/80 py-4 backdrop-blur-md transition-colors duration-500 dark:border-slate-800 dark:bg-slate-900/80">
      <div className="flex gap-2 overflow-x-auto pb-2 px-1 scrollbar-hide">
        <button
          onClick={() => onSelectCategory('all')}
          className={`px-5 py-2 rounded-full whitespace-nowrap text-sm font-medium transition-all duration-300 ${
            selectedCategory === 'all'
              ? 'bg-stone-900 dark:bg-white text-white dark:text-stone-900 shadow-md shadow-stone-900/15'
              : 'bg-stone-100 dark:bg-slate-800 text-stone-600 dark:text-gray-400 hover:bg-stone-200 dark:hover:bg-slate-700'
          }`}
        >
          Tất cả
        </button>

        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => onSelectCategory(category.id)}
            className={`px-5 py-2 rounded-full whitespace-nowrap text-sm font-medium transition-all duration-300 ${
              selectedCategory === category.id
                ? 'bg-stone-900 dark:bg-white text-white dark:text-stone-900 shadow-md shadow-stone-900/15'
                : 'bg-stone-100 dark:bg-slate-800 text-stone-600 dark:text-gray-400 hover:bg-stone-200 dark:hover:bg-slate-700'
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
