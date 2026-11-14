import { AlertCircle } from 'lucide-react';

const ProfileFieldError = ({ message }) => (
  <p className="flex items-center gap-1.5 text-xs font-semibold text-rose-500 dark:text-rose-400">
    <AlertCircle size={13} className="shrink-0" />
    {message}
  </p>
);

export default ProfileFieldError;
