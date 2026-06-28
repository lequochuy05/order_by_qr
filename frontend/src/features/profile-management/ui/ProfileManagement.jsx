import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';

import { useAuth } from '@features/auth';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { profileService } from '../api/profileService.js';
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
  const { showError } = useStatusModal();
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
  const initialProfileRef = useRef(null);

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
      initialProfileRef.current = nextProfile;
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

  const profileChanged =
    profile.fullName !== initialProfileRef.current?.fullName ||
    profile.phone !== initialProfileRef.current?.phone;

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

    setAvatarPreview(URL.createObjectURL(file));
    setUploadingAvatar(true);
    try {
      const updated = await profileService.uploadAvatar(file);
      setProfile((prev) => ({ ...prev, avatarUrl: updated.avatarUrl || '' }));
      setAvatarPreview(updated.avatarUrl || '');
      updateUser({ avatarUrl: updated.avatarUrl || '' });
      showSuccessToast('Ảnh đại diện đã được cập nhật.');
    } catch (err) {
      setAvatarPreview(profile.avatarUrl || '');
      showErrorToast(err);
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
    if (passwordForm.newPassword.length < 8) {
      nextErrors.newPassword = 'Mật khẩu mới phải có ít nhất 8 ký tự';
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
      showSuccessToast('Mật khẩu đã được thay đổi.');
    } catch (err) {
      showErrorToast(err);
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
  );
};

export default ProfilePage;
