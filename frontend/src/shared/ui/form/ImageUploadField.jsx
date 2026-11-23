import { Camera, ImageIcon, Upload, User } from 'lucide-react';

import FormLabel from './FormLabel.jsx';

const ImageUploadField = ({
  label = 'Hình ảnh',
  preview,
  onChange,
  inputId,
  variant = 'cover',
  helperText = 'Click để tải ảnh lên (Max 5MB)',
  changeLabel = 'Thay đổi',
  className = '',
}) => {
  if (variant === 'avatar') {
    return (
      <div className={`flex justify-center ${className}`}>
        <div className="group relative">
          <div className="h-28 w-28 overflow-hidden rounded-full border-4 border-white bg-slate-100 shadow-lg ring-1 ring-slate-200/80 transition-all group-hover:ring-indigo-400/50 dark:border-slate-800 dark:bg-slate-950 dark:ring-slate-700/80">
            {preview ? (
              <img src={preview} alt="Preview" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-slate-400 dark:text-slate-500">
                <User size={44} />
              </div>
            )}
          </div>
          <label
            htmlFor={inputId}
            className="absolute bottom-0 right-0 flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border-2 border-white bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/30 transition-transform hover:scale-110 active:scale-95 dark:border-slate-900"
            aria-label={changeLabel}
          >
            <Camera size={16} />
          </label>
          <input id={inputId} type="file" accept="image/*" className="hidden" onChange={onChange} />
        </div>
      </div>
    );
  }

  return (
    <div className={className}>
      {label && <FormLabel>{label}</FormLabel>}
      <div
        className={`group relative flex h-40 flex-col items-center justify-center overflow-hidden rounded-2xl border-2 border-dashed p-2 text-center transition-all duration-300 ${
          preview
            ? 'border-indigo-300/80 bg-indigo-50/20 dark:border-indigo-500/35 dark:bg-indigo-500/10'
            : 'border-slate-200/90 bg-slate-50/70 hover:border-indigo-400/80 hover:bg-indigo-50/25 dark:border-slate-800 dark:bg-slate-950/60 dark:hover:border-indigo-500/50 dark:hover:bg-indigo-950/20'
        }`}
      >
        {preview ? (
          <img
            src={preview}
            className="h-full w-full rounded-xl object-cover shadow-sm"
            alt="Preview"
          />
        ) : (
          <div className="flex flex-col items-center space-y-2.5 py-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-indigo-500 shadow-xs ring-1 ring-slate-200/80 transition-transform duration-300 group-hover:scale-110 dark:bg-slate-900 dark:text-indigo-400 dark:ring-slate-700/80">
              <ImageIcon size={22} />
            </div>
            <div className="space-y-0.5">
              <p className="text-xs font-bold text-slate-600 dark:text-slate-300">Tải ảnh lên</p>
              <p className="text-[11px] font-medium text-slate-400 dark:text-slate-500">{helperText}</p>
            </div>
          </div>
        )}
        <input type="file" id={inputId} className="hidden" accept="image/*" onChange={onChange} />
        <label
          htmlFor={inputId}
          className="absolute inset-0 flex cursor-pointer items-center justify-center bg-slate-950/45 opacity-0 backdrop-blur-[2px] transition-all duration-200 group-hover:opacity-100"
        >
          <span className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-xs font-bold text-slate-800 shadow-xl transition-transform hover:scale-105 active:scale-95 dark:bg-slate-900 dark:text-slate-100">
            <Upload size={14} className="text-indigo-600 dark:text-indigo-400" />
            {preview ? changeLabel : 'Chọn tệp ảnh'}
          </span>
        </label>
      </div>
    </div>
  );
};

export default ImageUploadField;
