import { ImageIcon, Upload, User } from 'lucide-react';

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
          <div className="h-28 w-28 overflow-hidden rounded-full border-4 border-white bg-gray-100 shadow-lg">
            {preview ? (
              <img src={preview} alt="Preview" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-gray-400">
                <User size={48} />
              </div>
            )}
          </div>
          <label
            htmlFor={inputId}
            className="absolute bottom-0 right-0 cursor-pointer rounded-full bg-orange-500 p-2 text-white shadow-md transition-transform hover:scale-110 hover:bg-orange-600"
            aria-label={changeLabel}
          >
            <Upload size={18} />
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
        className={`group relative flex h-40 flex-col items-center justify-center overflow-hidden rounded-3xl border-2 border-dashed p-2 text-center transition-all duration-300 ${
          preview
            ? 'border-orange-200 bg-orange-50/20'
            : 'border-gray-100 bg-gray-50/50 hover:border-orange-200'
        }`}
      >
        {preview ? (
          <img
            src={preview}
            className="h-full w-full rounded-[1.25rem] object-cover shadow-sm"
            alt="Preview"
          />
        ) : (
          <div className="space-y-2 py-4 text-gray-300">
            <ImageIcon className="mx-auto" size={36} />
            <p className="text-[9px] font-black uppercase tracking-tighter">{helperText}</p>
          </div>
        )}
        <input type="file" id={inputId} className="hidden" accept="image/*" onChange={onChange} />
        <label
          htmlFor={inputId}
          className="absolute inset-0 flex cursor-pointer items-center justify-center bg-black/40 opacity-0 backdrop-blur-[2px] transition-all duration-300 group-hover:opacity-100"
        >
          <span className="rounded-full bg-white px-5 py-2 text-[10px] font-black text-gray-800 shadow-xl transition-all hover:scale-105 active:scale-95">
            {preview ? changeLabel : 'Tải ảnh lên'}
          </span>
        </label>
      </div>
    </div>
  );
};

export default ImageUploadField;
