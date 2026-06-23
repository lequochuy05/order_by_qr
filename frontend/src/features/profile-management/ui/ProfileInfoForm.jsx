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
  <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
    <div className="mb-6 flex min-w-0 items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
        <User size={20} />
      </div>
      <div className="min-w-0">
        <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">Thông tin cá nhân</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Cập nhật thông tin hiển thị trong hệ thống quản trị.
        </p>
      </div>
    </div>

    <form onSubmit={onSubmit} className="grid gap-5 md:grid-cols-2">
      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Họ và tên</span>
        <div className="relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            value={profile.fullName}
            onChange={(event) => setProfile((prev) => ({ ...prev, fullName: event.target.value }))}
            className={`w-full rounded-lg border bg-white py-3 pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-100 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20 ${
              errors.fullName
                ? 'border-red-300 focus:border-red-400'
                : 'border-slate-200 focus:border-orange-400'
            } dark:border-slate-700`}
          />
        </div>
        {errors.fullName && <ProfileFieldError message={errors.fullName} />}
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Số điện thoại</span>
        <div className="relative">
          <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            value={profile.phone}
            onChange={(event) => setProfile((prev) => ({ ...prev, phone: event.target.value }))}
            placeholder="VD: 0912345678"
            className={`w-full rounded-lg border bg-white py-3 pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-100 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20 ${
              errors.phone
                ? 'border-red-300 focus:border-red-400'
                : 'border-slate-200 focus:border-orange-400'
            } dark:border-slate-700`}
          />
        </div>
        {errors.phone && <ProfileFieldError message={errors.phone} />}
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">
          Email đăng nhập
        </span>
        <div className="relative">
          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            value={profile.email}
            disabled
            className="w-full cursor-not-allowed rounded-lg border border-slate-200 bg-slate-50 py-3 pl-10 pr-3 text-sm text-slate-500 outline-none dark:border-slate-700 dark:bg-slate-950 dark:text-slate-400"
          />
        </div>
      </label>

      <label className="space-y-2">
        <span className="text-sm font-bold text-slate-700 dark:text-slate-200">Vai trò</span>
        <div className="relative">
          <ShieldCheck
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            size={18}
          />
          <input
            value={fmtRole(profile.role)}
            disabled
            className="w-full cursor-not-allowed rounded-lg border border-slate-200 bg-slate-50 py-3 pl-10 pr-3 text-sm text-slate-500 outline-none dark:border-slate-700 dark:bg-slate-950 dark:text-slate-400"
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
