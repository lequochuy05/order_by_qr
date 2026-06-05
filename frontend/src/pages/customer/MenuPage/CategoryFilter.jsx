const CategoryFilter = ({ categories, selectedCategory, onSelectCategory, labels = { all: 'Tất cả' } }) => {
  return (
    <div className="sticky top-0 z-10 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md py-2 mb-2 border-b border-gray-100 dark:border-slate-800 transition-colors duration-500">
      <div className="flex gap-2 overflow-x-auto pb-1 px-1 scrollbar-hide">
        <button
          onClick={() => onSelectCategory('all')}
          className={`px-3.5 py-1.5 rounded-full whitespace-nowrap text-xs font-semibold transition-all duration-300 ${
            selectedCategory === 'all'
              ? 'bg-blue-600 dark:bg-blue-500 text-white shadow-md shadow-blue-200 dark:shadow-blue-900/30'
              : 'bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-slate-700'
          }`}
        >
          {labels.all}
        </button>
        
        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => onSelectCategory(category.id)}
            className={`px-3.5 py-1.5 rounded-full whitespace-nowrap text-xs font-semibold transition-all duration-300 ${
              selectedCategory === category.id
                ? 'bg-blue-600 dark:bg-blue-500 text-white shadow-md shadow-blue-200 dark:shadow-blue-900/30'
                : 'bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-slate-700'
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
