import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  Camera,
  CheckCircle2,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  Mail,
  Phone,
  Save,
  ShieldCheck,
  Upload,
  User,
} from 'lucide-react';

import { useAuth } from '@features/auth/model/AuthContext.jsx';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { profileService } from '@features/profile-management/api/profileService.js';
import { fmtRole } from '@shared/lib/formatters.js';

const initialProfile = {
  fullName: '',
  email: '',
  phone: '',
  role: '',
  avatarUrl: '',
};

const initialPassword = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

const passwordRules = [
  { label: '6+ ký tự', test: (value) => value.length >= 6 },
  { label: 'Chữ hoa', test: (value) => /[A-Z]/.test(value) },
  { label: 'Chữ thường', test: (value) => /[a-z]/.test(value) },
  { label: 'Số', test: (value) => /\d/.test(value) },
];

const strengthMeta = [
  { label: 'Yếu', barClass: 'bg-red-500', textClass: 'text-red-600' },
  { label: 'Trung bình', barClass: 'bg-orange-500', textClass: 'text-orange-600' },
  { label: 'Tốt', barClass: 'bg-emerald-500', textClass: 'text-emerald-600' },
  { label: 'Mạnh', barClass: 'bg-emerald-600', textClass: 'text-emerald-700' },
];

const ProfilePage = () => {
  const { user, updateUser } = useAuth();
  const { showSuccess, showError } = useStatusModal();
  const fileInputRef = useRef(null);

  const [profile, setProfile] = useState(initialProfile);
  const [avatarPreview, setAvatarPreview] = useState('');
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [passwordForm, setPasswordForm] = useState(initialPassword);
  const [savingPassword, setSavingPassword] = useState(false);
  const [showPasswords, setShowPasswords] = useState(false);
  const [errors, setErrors] = useState({});

  const isMountedRef = useRef(true);
  const fetchSeqRef = useRef(0);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const loadProfile = useCallback(async () => {
    const fetchSeq = ++fetchSeqRef.current;
    setLoading(true);
    try {
      const data = await profileService.getMe();
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;

      const nextProfile = {
        fullName: data.fullName || '',
        email: data.email || '',
        phone: data.phone || '',
        role: data.role || '',
        avatarUrl: data.avatarUrl || '',
      };
      setProfile(nextProfile);
      setAvatarPreview(nextProfile.avatarUrl);
      updateUser({
        fullName: nextProfile.fullName,
        email: nextProfile.email,
        phone: nextProfile.phone,
        role: nextProfile.role,
        avatarUrl: nextProfile.avatarUrl,
      });
    } catch (err) {
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      showError(err, 'Không thể tải hồ sơ');
    } finally {
      if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
        setLoading(false);
      }
    }
  }, [showError, updateUser]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const strength = useMemo(() => {
    const passed = passwordRules.filter((rule) => rule.test(passwordForm.newPassword)).length;
    if (!passwordForm.newPassword) return { passed: 0, meta: strengthMeta[0] };
    return {
      passed,
      meta: strengthMeta[Math.min(Math.max(passed - 1, 0), strengthMeta.length - 1)],
    };
  }, [passwordForm.newPassword]);

  const validateProfile = () => {
    const nextErrors = {};
    if (!profile.fullName.trim()) {
      nextErrors.fullName = 'Họ và tên không được để trống';
    } else if (profile.fullName.trim().length < 2) {
      nextErrors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    }

    if (profile.phone && !/^(84|0)[0-9]{9}\b/.test(profile.phone)) {
      nextErrors.phone = 'Số điện thoại không hợp lệ';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    if (!validateProfile() || savingProfile) return;

    setSavingProfile(true);
    try {
      const updated = await profileService.updateMe({
        fullName: profile.fullName.trim(),
        phone: profile.phone.trim() || null,
      });
      setProfile((prev) => ({ ...prev, ...updated, phone: updated.phone || '' }));
      updateUser({
        fullName: updated.fullName,
        phone: updated.phone || '',
        avatarUrl: updated.avatarUrl,
        role: updated.role,
        email: updated.email,
      });
      showSuccess('Thông tin cá nhân đã được cập nhật.');
    } catch (err) {
      showError(err);
    } finally {
      setSavingProfile(false);
    }
  };

  const handleAvatarChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      showError('Vui lòng chọn một file hình ảnh.', 'File không hợp lệ');
      return;
    }

    setAvatarPreview(URL.createObjectURL(file));
    setUploadingAvatar(true);
    try {
      const updated = await profileService.uploadAvatar(file);
      setProfile((prev) => ({ ...prev, avatarUrl: updated.avatarUrl || '' }));
      setAvatarPreview(updated.avatarUrl || '');
      updateUser({ avatarUrl: updated.avatarUrl || '' });
      showSuccess('Ảnh đại diện đã được cập nhật.');
    } catch (err) {
      setAvatarPreview(profile.avatarUrl || '');
      showError(err, 'Không thể cập nhật ảnh');
    } finally {
      setUploadingAvatar(false);
      event.target.value = '';
    }
  };

  const validatePassword = () => {
    const nextErrors = {};
    if (!passwordForm.currentPassword) {
      nextErrors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại';
    }
    if (passwordForm.newPassword.length < 6) {
      nextErrors.newPassword = 'Mật khẩu mới phải có ít nhất 6 ký tự';
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      nextErrors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp';
    }
    if (passwordForm.currentPassword && passwordForm.currentPassword === passwordForm.newPassword) {
      nextErrors.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    if (!validatePassword() || savingPassword) return;

    setSavingPassword(true);
    try {
      await profileService.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm(initialPassword);
      showSuccess('Mật khẩu đã được thay đổi.');
    } catch (err) {
      showError(err);
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center text-orange-500 dark:text-orange-400">
        <Loader2 className="animate-spin" size={36} />
      </div>
    );
  }

  return (
    <div className="mx-auto min-h-screen w-full min-w-0 max-w-7xl space-y-4 bg-slate-50 p-0 animate-in fade-in duration-500 sm:space-y-6 sm:p-3 lg:p-6">
      <div className="grid min-w-0 gap-4 sm:gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
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
                {uploadingAvatar ? (
                  <Loader2 className="animate-spin" size={18} />
                ) : (
                  <Camera size={18} />
                )}
              </button>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleAvatarChange}
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

        <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-6 flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
              <User size={20} />
            </div>
            <div className="min-w-0">
              <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">
                Thông tin cá nhân
              </h2>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Cập nhật thông tin hiển thị trong hệ thống quản trị.
              </p>
            </div>
          </div>

          <form onSubmit={handleProfileSubmit} className="grid gap-5 md:grid-cols-2">
            <label className="space-y-2">
              <span className="text-sm font-bold text-slate-700 dark:text-slate-200">
                Họ và tên
              </span>
              <div className="relative">
                <User
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  size={18}
                />
                <input
                  value={profile.fullName}
                  onChange={(event) =>
                    setProfile((prev) => ({ ...prev, fullName: event.target.value }))
                  }
                  className={`w-full rounded-lg border bg-white py-3 pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-100 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20 ${
                    errors.fullName
                      ? 'border-red-300 focus:border-red-400'
                      : 'border-slate-200 focus:border-orange-400'
                  } dark:border-slate-700`}
                />
              </div>
              {errors.fullName && <FieldError message={errors.fullName} />}
            </label>

            <label className="space-y-2">
              <span className="text-sm font-bold text-slate-700 dark:text-slate-200">
                Số điện thoại
              </span>
              <div className="relative">
                <Phone
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  size={18}
                />
                <input
                  value={profile.phone}
                  onChange={(event) =>
                    setProfile((prev) => ({ ...prev, phone: event.target.value }))
                  }
                  placeholder="VD: 0912345678"
                  className={`w-full rounded-lg border bg-white py-3 pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-100 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20 ${
                    errors.phone
                      ? 'border-red-300 focus:border-red-400'
                      : 'border-slate-200 focus:border-orange-400'
                  } dark:border-slate-700`}
                />
              </div>
              {errors.phone && <FieldError message={errors.phone} />}
            </label>

            <label className="space-y-2">
              <span className="text-sm font-bold text-slate-700 dark:text-slate-200">
                Email đăng nhập
              </span>
              <div className="relative">
                <Mail
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  size={18}
                />
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
              <button
                type="submit"
                disabled={savingProfile}
                className="inline-flex items-center gap-2 rounded-lg bg-orange-500 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {savingProfile ? (
                  <Loader2 className="animate-spin" size={17} />
                ) : (
                  <Save size={17} />
                )}
                Lưu thay đổi
              </button>
            </div>
          </form>
        </section>
      </div>

      <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-colors sm:p-6 dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-6 flex min-w-0 items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
              <Lock size={20} />
            </div>
            <div className="min-w-0">
              <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">
                Đổi mật khẩu
              </h2>
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

        <form onSubmit={handlePasswordSubmit} className="grid gap-5 lg:grid-cols-3">
          <PasswordField
            label="Mật khẩu hiện tại"
            value={passwordForm.currentPassword}
            visible={showPasswords}
            error={errors.currentPassword}
            onChange={(value) => setPasswordForm((prev) => ({ ...prev, currentPassword: value }))}
          />
          <PasswordField
            label="Mật khẩu mới"
            value={passwordForm.newPassword}
            visible={showPasswords}
            error={errors.newPassword}
            onChange={(value) => setPasswordForm((prev) => ({ ...prev, newPassword: value }))}
          />
          <PasswordField
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
    </div>
  );
};

const FieldError = ({ message }) => (
  <p className="flex items-center gap-1 text-xs font-medium text-red-500">
    <AlertCircle size={13} />
    {message}
  </p>
);

const PasswordField = ({ label, value, visible, error, onChange }) => (
  <label className="min-w-0 space-y-2">
    <span className="text-sm font-bold text-slate-700 dark:text-slate-200">{label}</span>
    <div className="relative">
      <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
      <input
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={`w-full rounded-lg border bg-white py-3 pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-100 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20 ${
          error ? 'border-red-300 focus:border-red-400' : 'border-slate-200 focus:border-orange-400'
        } dark:border-slate-700`}
      />
    </div>
    {error && <FieldError message={error} />}
  </label>
);

export default ProfilePage;
