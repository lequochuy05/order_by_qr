import { CheckCircle2, Eye, EyeOff, Loader2, Lock } from 'lucide-react';

import ProfilePasswordField from './ProfilePasswordField.jsx';

const ProfilePasswordForm = ({
  passwordForm,
  setPasswordForm,
  errors,
  savingPassword,
  showPasswords,
  setShowPasswords,
  strength,
  passwordRules,
  onSubmit,
}) => (
  <section className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="mb-6 flex min-w-0 items-center justify-between gap-3">
      <div className="flex min-w-0 items-center gap-3.5">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/25">
          <Lock size={22} />
        </div>
        <div className="min-w-0">
          <h2 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">Đổi mật khẩu</h2>
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
            Yêu cầu mật khẩu hiện tại để bảo vệ tài khoản.
          </p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setShowPasswords((prev) => !prev)}
        className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200/80 text-slate-500 transition-all hover:bg-slate-100 hover:text-slate-900 active:scale-95 dark:border-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
        title={showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
      >
        {showPasswords ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>

    <form onSubmit={onSubmit} className="grid gap-5 lg:grid-cols-3">
      <ProfilePasswordField
        label="Mật khẩu hiện tại"
        value={passwordForm.currentPassword}
        visible={showPasswords}
        error={errors.currentPassword}
        onChange={(value) => setPasswordForm((prev) => ({ ...prev, currentPassword: value }))}
      />
      <ProfilePasswordField
        label="Mật khẩu mới"
        value={passwordForm.newPassword}
        visible={showPasswords}
        error={errors.newPassword}
        onChange={(value) => setPasswordForm((prev) => ({ ...prev, newPassword: value }))}
      />
      <ProfilePasswordField
        label="Xác nhận mật khẩu"
        value={passwordForm.confirmPassword}
        visible={showPasswords}
        error={errors.confirmPassword}
        onChange={(value) => setPasswordForm((prev) => ({ ...prev, confirmPassword: value }))}
      />

      <div className="lg:col-span-3">
        <div className="mb-6 grid gap-3 md:grid-cols-[260px_1fr]">
          <div>
            <div className="mb-2 flex items-center justify-between text-xs font-bold">
              <span className="text-slate-500 dark:text-slate-400">Độ mạnh mật khẩu</span>
              <span className={strength.meta.textClass}>
                {passwordForm.newPassword ? strength.meta.label : 'Chưa nhập'}
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
              <div
                className={`h-full rounded-full transition-all duration-300 ${strength.meta.barClass}`}
                style={{
                  width: `${Math.max(strength.passed, passwordForm.newPassword ? 1 : 0) * 25}%`,
                }}
              />
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            {passwordRules.map((rule) => {
              const passed = rule.test(passwordForm.newPassword);
              return (
                <span
                  key={rule.label}
                  className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-bold ${
                    passed
                      ? 'border-emerald-200/60 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-950/40 dark:text-emerald-300'
                      : 'border-slate-200/80 bg-slate-50 text-slate-500 dark:border-slate-800 dark:bg-slate-900/60 dark:text-slate-400'
                  }`}
                >
                  <CheckCircle2 size={13} />
                  {rule.label}
                </span>
              );
            })}
          </div>
        </div>

        <button
          type="submit"
          disabled={savingPassword}
          className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-6 py-3 text-sm font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 hover:shadow-lg hover:shadow-indigo-600/30 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {savingPassword ? <Loader2 className="animate-spin" size={17} /> : <Lock size={17} />}
          Cập nhật mật khẩu
        </button>
      </div>
    </form>
  </section>
);

export default ProfilePasswordForm;
