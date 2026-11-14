import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AlertTriangle, Loader2 } from 'lucide-react';

import { useAuth } from '@features/auth';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import {
  useChangePasswordMutation,
  useProfileQuery,
  useUpdateProfileMutation,
  useUploadProfileAvatarMutation,
} from '@entities/user';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

import ProfileAvatarCard from './ProfileAvatarCard.jsx';
import ProfileInfoForm from './ProfileInfoForm.jsx';
import ProfilePasswordForm from './ProfilePasswordForm.jsx';

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
  { label: '8+ ký tự', test: (value) => value.length >= 8 },
  { label: 'Chữ hoa', test: (value) => /[A-Z]/.test(value) },
  { label: 'Số', test: (value) => /\d/.test(value) },
  { label: 'Ký tự đặc biệt', test: (value) => /[!@#$%^&*(),.?":{}|<>]/.test(value) },
];

const hasValidPasswordPolicy = (value) => passwordRules.every((rule) => rule.test(value));

const strengthMeta = [
  { label: 'Yếu', barClass: 'bg-rose-500', textClass: 'text-rose-600' },
  { label: 'Trung bình', barClass: 'bg-amber-500', textClass: 'text-amber-600' },
  { label: 'Tốt', barClass: 'bg-emerald-500', textClass: 'text-emerald-600 dark:text-emerald-400' },
  { label: 'Mạnh', barClass: 'bg-emerald-600', textClass: 'text-emerald-700' },
];

const ProfilePage = () => {
  const { user, updateUser } = useAuth();
  const [searchParams] = useSearchParams();
  const isForceChange = searchParams.get('forceChangePassword') === 'true' || Boolean(user?.mustChangePassword);
  const passwordSectionRef = useRef(null);
  const { showError } = useStatusModal();
  const fileInputRef = useRef(null);
  const avatarObjectUrlRef = useRef(null);

  const [profile, setProfile] = useState(initialProfile);
  const { data: profileData, isLoading: loading } = useProfileQuery();
  const { mutateAsync: updateProfileMut } = useUpdateProfileMutation();
  const { mutateAsync: uploadAvatarMut } = useUploadProfileAvatarMutation();
  const { mutateAsync: changePasswordMut } = useChangePasswordMutation();
  const [avatarPreview, setAvatarPreview] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [passwordForm, setPasswordForm] = useState(initialPassword);
  const [savingPassword, setSavingPassword] = useState(false);
  const [showPasswords, setShowPasswords] = useState(false);
  const [errors, setErrors] = useState({});

  const isMountedRef = useRef(true);
  const initialProfileRef = useRef(null);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
      if (avatarObjectUrlRef.current) {
        URL.revokeObjectURL(avatarObjectUrlRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (profileData) {
      const nextProfile = {
        fullName: profileData.fullName || '',
        email: profileData.email || '',
        phone: profileData.phone || '',
        role: profileData.role || '',
        avatarUrl: profileData.avatarUrl || '',
      };
      setProfile(nextProfile);
      initialProfileRef.current = nextProfile;
      setAvatarPreview(nextProfile.avatarUrl);
      updateUser({
        fullName: nextProfile.fullName,
        email: nextProfile.email,
        phone: nextProfile.phone,
        role: nextProfile.role,
        avatarUrl: nextProfile.avatarUrl,
        mustChangePassword: Boolean(profileData.mustChangePassword),
      });
    }
  }, [profileData, updateUser]);

  useEffect(() => {
    if (isForceChange && passwordSectionRef.current) {
      passwordSectionRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [isForceChange]);

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

  const profileChanged =
    profile.fullName !== initialProfileRef.current?.fullName ||
    profile.phone !== initialProfileRef.current?.phone;

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    if (!validateProfile() || savingProfile) return;

    setSavingProfile(true);
    try {
      const updated = await updateProfileMut({
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
      showSuccessToast('Thông tin cá nhân đã được cập nhật.');
    } catch (err) {
      showErrorToast(err);
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

    if (avatarObjectUrlRef.current) {
      URL.revokeObjectURL(avatarObjectUrlRef.current);
    }
    const objectUrl = URL.createObjectURL(file);
    avatarObjectUrlRef.current = objectUrl;
    setAvatarPreview(objectUrl);
    setUploadingAvatar(true);
    try {
      const updated = await uploadAvatarMut(file);
      setProfile((prev) => ({ ...prev, avatarUrl: updated.avatarUrl || '' }));
      setAvatarPreview(updated.avatarUrl || '');
      updateUser({ avatarUrl: updated.avatarUrl || '' });
      showSuccessToast('Ảnh đại diện đã được cập nhật.');
    } catch (err) {
      setAvatarPreview(profile.avatarUrl || '');
      showErrorToast(err);
    } finally {
      if (avatarObjectUrlRef.current) {
        URL.revokeObjectURL(avatarObjectUrlRef.current);
        avatarObjectUrlRef.current = null;
      }
      setUploadingAvatar(false);
      event.target.value = '';
    }
  };

  const validatePassword = () => {
    const nextErrors = {};
    if (!passwordForm.currentPassword) {
      nextErrors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại';
    }
    if (!hasValidPasswordPolicy(passwordForm.newPassword)) {
      nextErrors.newPassword = 'Mật khẩu cần 8 ký tự, chữ hoa, số và ký tự đặc biệt';
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
      await changePasswordMut({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm(initialPassword);
      updateUser({ mustChangePassword: false });
      showSuccessToast('Mật khẩu đã được thay đổi thành công.');
    } catch (err) {
      showErrorToast(err);
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center text-indigo-600 dark:text-indigo-400">
        <Loader2 className="animate-spin" size={36} />
      </div>
    );
  }

  return (
    <div className="mx-auto w-full min-w-0 max-w-7xl space-y-6 p-0 sm:p-3 lg:p-6 animate-in fade-in duration-500">
      {isForceChange && (
        <div className="flex items-start gap-3.5 rounded-3xl border border-amber-200/80 bg-amber-50/90 p-5 text-amber-900 shadow-sm dark:border-amber-800/50 dark:bg-amber-950/40 dark:text-amber-200">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-600 dark:bg-amber-900/40 dark:text-amber-400">
            <AlertTriangle size={20} />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-amber-900 dark:text-amber-100">
              Yêu cầu đổi mật khẩu bảo mật
            </h3>
            <p className="text-sm font-medium text-amber-800/90 dark:text-amber-300/90">
              Tài khoản của bạn vừa được cấp mới hoặc đặt lại mật khẩu. Vui lòng đổi sang mật khẩu mới trước khi tiếp tục thao tác trên hệ thống.
            </p>
          </div>
        </div>
      )}

      <div className="grid min-w-0 gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
        <ProfileAvatarCard
          profile={profile}
          user={user}
          avatarPreview={avatarPreview}
          uploadingAvatar={uploadingAvatar}
          fileInputRef={fileInputRef}
          onAvatarChange={handleAvatarChange}
        />

        <ProfileInfoForm
          profile={profile}
          setProfile={setProfile}
          errors={errors}
          savingProfile={savingProfile}
          profileChanged={profileChanged}
          onSubmit={handleProfileSubmit}
        />
      </div>

      <div ref={passwordSectionRef} id="password-form-section">
        <ProfilePasswordForm
          passwordForm={passwordForm}
          setPasswordForm={setPasswordForm}
          errors={errors}
          savingPassword={savingPassword}
          showPasswords={showPasswords}
          setShowPasswords={setShowPasswords}
          strength={strength}
          passwordRules={passwordRules}
          onSubmit={handlePasswordSubmit}
        />
      </div>
    </div>
  );
};

export default ProfilePage;
