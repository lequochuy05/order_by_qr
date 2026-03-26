const CategoryFilter = ({ categories, selectedCategory, onSelectCategory }) => {
  return (
    <div className="sticky top-0 z-10 bg-white/80 backdrop-blur-md py-4 mb-4 border-b border-gray-100">
      <div className="flex gap-2 overflow-x-auto pb-2 px-1 scrollbar-hide">
        <button
          onClick={() => onSelectCategory('all')}
          className={`px-5 py-2 rounded-full whitespace-nowrap text-sm font-medium transition-all ${
            selectedCategory === 'all'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-200'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          Tất cả
        </button>
        
        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => onSelectCategory(category.id)}
            className={`px-5 py-2 rounded-full whitespace-nowrap text-sm font-medium transition-all ${
              selectedCategory === category.id
                ? 'bg-blue-600 text-white shadow-md shadow-blue-200'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
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