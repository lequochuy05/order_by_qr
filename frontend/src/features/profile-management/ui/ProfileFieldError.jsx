import { AlertCircle } from 'lucide-react';

const ProfileFieldError = ({ message }) => (
  <p className="flex items-center gap-1 text-xs font-medium text-red-500">
    <AlertCircle size={13} />
    {message}
  </p>
);

export default ProfileFieldError;
