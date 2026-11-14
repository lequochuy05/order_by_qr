import { Camera, Loader2, ShieldCheck, Upload, User } from 'lucide-react';

import { fmtRole } from '@shared/lib/formatters.js';

const ProfileAvatarCard = ({
  profile,
  user,
  avatarPreview,
  uploadingAvatar,
  fileInputRef,
  onAvatarChange,
}) => (
  <section className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="flex flex-col items-center text-center">
      <div className="relative">
        <div className="h-36 w-36 overflow-hidden rounded-full border-4 border-indigo-100 bg-indigo-50/50 shadow-inner dark:border-indigo-500/20 dark:bg-slate-950">
          {avatarPreview ? (
            <img src={avatarPreview} alt="Avatar" className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <User className="text-indigo-500 dark:text-indigo-400" size={56} />
            </div>
          )}
        </div>
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadingAvatar}
          className="absolute bottom-1 right-1 flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-600/30 transition-all hover:scale-105 hover:shadow-indigo-600/40 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
          title="Đổi ảnh đại diện"
        >
          {uploadingAvatar ? <Loader2 className="animate-spin" size={18} /> : <Camera size={18} />}
        </button>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={onAvatarChange}
      />

      <h2 className="mt-5 text-xl font-black tracking-tight text-slate-900 dark:text-white">
        {profile.fullName || user?.fullName}
      </h2>
      <div className="mt-2 inline-flex items-center gap-2 rounded-full border border-indigo-200/60 bg-indigo-50/80 px-3.5 py-1 text-xs font-bold uppercase tracking-wider text-indigo-700 shadow-sm dark:border-indigo-500/30 dark:bg-indigo-950/40 dark:text-indigo-300">
        <ShieldCheck size={14} />
        {fmtRole(profile.role || user?.role)}
      </div>

      <button
        type="button"
        onClick={() => fileInputRef.current?.click()}
        disabled={uploadingAvatar}
        className="mt-6 inline-flex items-center gap-2 rounded-xl border border-slate-200/80 px-4 py-2.5 text-sm font-bold text-slate-700 shadow-sm transition-all hover:border-indigo-300 hover:bg-indigo-50/50 hover:text-indigo-600 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:border-indigo-500/40 dark:hover:bg-indigo-950/30 dark:hover:text-indigo-300"
      >
        <Upload size={16} />
        Chọn ảnh mới
      </button>
    </div>
  </section>
);

export default ProfileAvatarCard;
