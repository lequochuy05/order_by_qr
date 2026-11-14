import { Lock } from 'lucide-react';

import ProfileFieldError from './ProfileFieldError.jsx';

const ProfilePasswordField = ({ label, value, visible, error, onChange }) => (
  <label className="min-w-0 space-y-2">
    <span className="text-sm font-bold text-slate-700 dark:text-slate-200">{label}</span>
    <div className="relative">
      <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={18} />
      <input
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={`w-full rounded-2xl border bg-slate-50/50 py-3 pl-11 pr-4 text-sm font-medium text-slate-900 outline-none transition-all focus:bg-white focus:ring-2 focus:ring-indigo-500/20 dark:bg-slate-950/60 dark:text-slate-100 dark:focus:ring-indigo-500/20 ${
          error
            ? 'border-rose-300 focus:border-rose-500 dark:border-rose-500/50'
            : 'border-slate-200/80 focus:border-indigo-500 dark:border-slate-800 dark:focus:border-indigo-400'
        }`}
      />
    </div>
    {error && <ProfileFieldError message={error} />}
  </label>
);

export default ProfilePasswordField;
