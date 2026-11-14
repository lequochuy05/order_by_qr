import { Mail, Phone, ShieldCheck, User } from 'lucide-react';

import { fmtRole } from '@shared/lib/formatters.js';
import { SaveButton } from '@shared/ui';

import ProfileFieldError from './ProfileFieldError.jsx';

const ProfileInfoForm = ({
  profile,
  setProfile,
  errors,
  savingProfile,
  profileChanged,
  onSubmit,
}) => (
  <section className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="mb-6 flex min-w-0 items-center gap-3.5">
      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
        <User size={22} />
      </div>
      <div className="min-w-0">
        <h2 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">Thông tin cá nhân</h2>
        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
          Cập nhật thông tin hiển thị trong hệ thống quản trị.
        </p>
      </div>
    </div>

    <form onSubmit={onSubmit} className="grid gap-5 md:grid-cols-2">
      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Họ và tên</span>
        <div className="relative">
          <User className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={18} />
          <input
            value={profile.fullName}
            onChange={(event) => setProfile((prev) => ({ ...prev, fullName: event.target.value }))}
            className={`w-full rounded-2xl border bg-slate-50/50 py-3 pl-11 pr-4 text-sm font-medium text-slate-900 outline-none transition-all focus:bg-white focus:ring-2 focus:ring-indigo-500/20 dark:bg-slate-950/60 dark:text-slate-100 dark:focus:ring-indigo-500/20 ${
              errors.fullName
                ? 'border-rose-300 focus:border-rose-500 dark:border-rose-500/50'
                : 'border-slate-200/80 focus:border-indigo-500 dark:border-slate-800 dark:focus:border-indigo-400'
            }`}
          />
        </div>
        {errors.fullName && <ProfileFieldError message={errors.fullName} />}
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Số điện thoại</span>
        <div className="relative">
          <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={18} />
          <input
            value={profile.phone}
            onChange={(event) => setProfile((prev) => ({ ...prev, phone: event.target.value }))}
            placeholder="VD: 0912345678"
            className={`w-full rounded-2xl border bg-slate-50/50 py-3 pl-11 pr-4 text-sm font-medium text-slate-900 outline-none transition-all focus:bg-white focus:ring-2 focus:ring-indigo-500/20 dark:bg-slate-950/60 dark:text-slate-100 dark:focus:ring-indigo-500/20 ${
              errors.phone
                ? 'border-rose-300 focus:border-rose-500 dark:border-rose-500/50'
                : 'border-slate-200/80 focus:border-indigo-500 dark:border-slate-800 dark:focus:border-indigo-400'
            }`}
          />
        </div>
        {errors.phone && <ProfileFieldError message={errors.phone} />}
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">
          Email đăng nhập
        </span>
        <div className="relative">
          <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={18} />
          <input
            value={profile.email}
            disabled
            className="w-full cursor-not-allowed rounded-2xl border border-slate-200/80 bg-slate-100/70 py-3 pl-11 pr-4 text-sm font-medium text-slate-500 outline-none dark:border-slate-800 dark:bg-slate-950/80 dark:text-slate-400"
          />
        </div>
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Vai trò</span>
        <div className="relative">
          <ShieldCheck
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500"
            size={18}
          />
          <input
            value={fmtRole(profile.role)}
            disabled
            className="w-full cursor-not-allowed rounded-2xl border border-slate-200/80 bg-slate-100/70 py-3 pl-11 pr-4 text-sm font-medium text-slate-500 outline-none dark:border-slate-800 dark:bg-slate-950/80 dark:text-slate-400"
          />
        </div>
      </label>

      <div className="md:col-span-2">
        <SaveButton
          type="submit"
          saving={savingProfile}
          disabled={savingProfile || !profileChanged}
        />
      </div>
    </form>
  </section>
);

export default ProfileInfoForm;
