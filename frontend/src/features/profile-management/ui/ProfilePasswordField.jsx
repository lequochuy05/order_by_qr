import { Lock } from 'lucide-react';

import ProfileFieldError from './ProfileFieldError.jsx';

const ProfilePasswordField = ({ label, value, visible, error, onChange }) => (
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
    {error && <ProfileFieldError message={error} />}
  </label>
);

export default ProfilePasswordField;
