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
  <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
    <div className="mb-6 flex min-w-0 items-center justify-between gap-3">
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
          <Lock size={20} />
        </div>
        <div className="min-w-0">
          <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">Đổi mật khẩu</h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Yêu cầu mật khẩu hiện tại để bảo vệ tài khoản.
          </p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setShowPasswords((prev) => !prev)}
        className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
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
        <div className="mb-5 grid gap-3 md:grid-cols-[260px_1fr]">
          <div>
            <div className="mb-2 flex items-center justify-between text-xs font-bold">
              <span className="text-slate-500 dark:text-slate-400">Độ mạnh mật khẩu</span>
              <span className={strength.meta.textClass}>
                {passwordForm.newPassword ? strength.meta.label : 'Chưa nhập'}
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
              <div
                className={`h-full rounded-full transition-all ${strength.meta.barClass}`}
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
                  className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-semibold ${
                    passed
                      ? 'border-emerald-100 bg-emerald-50 text-emerald-700'
                      : 'border-slate-200 bg-slate-50 text-slate-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-400'
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
          className="inline-flex items-center gap-2 rounded-lg bg-slate-900 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950 dark:hover:bg-white"
        >
          {savingPassword ? <Loader2 className="animate-spin" size={17} /> : <Lock size={17} />}
          Cập nhật mật khẩu
        </button>
      </div>
    </form>
  </section>
);

export default ProfilePasswordForm;
