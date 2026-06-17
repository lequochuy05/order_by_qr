const MenuItemImage = ({ src, alt = '', className = '' }) => (
  <div className={`overflow-hidden bg-slate-100 dark:bg-slate-800 ${className}`}>
    {src ? (
      <img src={src} alt={alt} className="h-full w-full object-cover" />
    ) : (
      <div className="flex h-full min-h-24 items-center justify-center text-sm font-bold text-slate-400">
        No image
      </div>
    )}
  </div>
);

export default MenuItemImage;
