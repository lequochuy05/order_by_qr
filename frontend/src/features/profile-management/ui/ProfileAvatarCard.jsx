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
  <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
    <div className="flex flex-col items-center text-center">
      <div className="relative">
        <div className="h-36 w-36 overflow-hidden rounded-full border-4 border-orange-100 bg-orange-50 dark:border-orange-500/30 dark:bg-slate-950">
          {avatarPreview ? (
            <img src={avatarPreview} alt="Avatar" className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <User className="text-orange-500" size={56} />
            </div>
          )}
        </div>
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadingAvatar}
          className="absolute bottom-1 right-1 flex h-11 w-11 items-center justify-center rounded-full bg-orange-500 text-white shadow-lg transition hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-60"
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

      <h2 className="mt-5 text-xl font-black text-slate-900 dark:text-slate-100">
        {profile.fullName || user?.fullName}
      </h2>
      <div className="mt-2 inline-flex items-center gap-2 rounded-full border border-orange-100 bg-orange-50 px-3 py-1 text-xs font-bold uppercase tracking-wide text-orange-600 dark:border-orange-500/20 dark:bg-orange-500/10 dark:text-orange-300">
        <ShieldCheck size={14} />
        {fmtRole(profile.role || user?.role)}
      </div>

      <button
        type="button"
        onClick={() => fileInputRef.current?.click()}
        disabled={uploadingAvatar}
        className="mt-6 inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-orange-200 hover:bg-orange-50 hover:text-orange-600 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:border-orange-500/40 dark:hover:bg-orange-500/10 dark:hover:text-orange-300"
      >
        <Upload size={16} />
        Chọn ảnh mới
      </button>
    </div>
  </section>
);

export default ProfileAvatarCard;
