const CategoryFilter = ({ categories, selectedCategory, onSelectCategory }) => {
  return (
    <div className="mb-4 border-b border-[#E8DCD1] bg-[#F7F0E7]/90 py-3 backdrop-blur-md transition-colors duration-500 dark:border-[#352C26] dark:bg-[#14100E]/90">
      <div className="flex gap-2 overflow-x-auto pb-1 px-1 scrollbar-hide">
        <button
          onClick={() => onSelectCategory('all')}
          className={`px-4 py-2 rounded-full whitespace-nowrap text-xs font-bold transition-all duration-300 ${
            selectedCategory === 'all'
              ? 'bg-[#56372B] text-white shadow-md shadow-[#56372B]/20 dark:bg-[#C17C4A] dark:text-white dark:shadow-[#C17C4A]/25'
              : 'bg-[#EAE0D5] text-[#56372B] hover:bg-[#DDD2C6] dark:bg-[#231A15] dark:text-[#D8CCC4] dark:hover:bg-[#2F231D]'
          }`}
        >
          Tất cả
        </button>

        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => onSelectCategory(category.id)}
            className={`px-4 py-2 rounded-full whitespace-nowrap text-xs font-bold transition-all duration-300 ${
              selectedCategory === category.id
                ? 'bg-[#56372B] text-white shadow-md shadow-[#56372B]/20 dark:bg-[#C17C4A] dark:text-white dark:shadow-[#C17C4A]/25'
                : 'bg-[#EAE0D5] text-[#56372B] hover:bg-[#DDD2C6] dark:bg-[#231A15] dark:text-[#D8CCC4] dark:hover:bg-[#2F231D]'
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
