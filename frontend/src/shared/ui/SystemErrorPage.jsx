import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  CircleAlert,
  Clock,
  FileWarning,
  Home,
  LockKeyhole,
  LogIn,
  RefreshCcw,
  SearchX,
  ServerCrash,
  ShieldAlert,
  TimerReset,
  WifiOff,
} from 'lucide-react';

import { getSystemErrorContent } from '@shared/lib/systemErrors.js';

const statusIcons = {
  alertTriangle: AlertTriangle,
  circleAlert: CircleAlert,
  clock: Clock,
  fileWarning: FileWarning,
  lock: LockKeyhole,
  search: SearchX,
  serverCrash: ServerCrash,
  shield: ShieldAlert,
  timerReset: TimerReset,
  wifiOff: WifiOff,
};

const actionIcons = {
  back: ArrowLeft,
  home: Home,
  login: LogIn,
  refresh: RefreshCcw,
};

const toneStyles = {
  amber: {
    icon: 'bg-amber-50 text-amber-600 ring-amber-100 dark:bg-amber-500/10 dark:text-amber-300 dark:ring-amber-500/20',
    code: 'text-amber-600 dark:text-amber-300',
    eyebrow: 'text-amber-700 dark:text-amber-300',
    border: 'border-amber-100 dark:border-amber-500/20',
  },
  orange: {
    icon: 'bg-indigo-50 text-indigo-600 ring-indigo-100 dark:bg-indigo-500/10 dark:text-indigo-300 dark:ring-indigo-500/20',
    code: 'text-indigo-600 dark:text-indigo-300',
    eyebrow: 'text-indigo-700 dark:text-indigo-300',
    border: 'border-indigo-100 dark:border-indigo-500/20',
  },
  red: {
    icon: 'bg-rose-50 text-rose-600 ring-rose-100 dark:bg-rose-500/10 dark:text-rose-300 dark:ring-rose-500/20',
    code: 'text-rose-600 dark:text-rose-300',
    eyebrow: 'text-rose-700 dark:text-rose-300',
    border: 'border-rose-100 dark:border-rose-500/20',
  },
  slate: {
    icon: 'bg-slate-100 text-slate-700 ring-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:ring-slate-700',
    code: 'text-slate-800 dark:text-slate-100',
    eyebrow: 'text-slate-500 dark:text-slate-400',
    border: 'border-slate-200 dark:border-slate-700',
  },
};

const actionBaseClass =
  'inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto';

const actionVariants = {
  primary:
    'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-600/25 hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98]',
  secondary:
    'border border-slate-200/80 bg-white text-slate-700 hover:bg-slate-50 active:scale-[0.98] dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
};

const runClientAction = (action) => {
  if (action?.onClick) {
    action.onClick();
    return;
  }

  if (action?.type === 'reload') {
    window.location.reload();
    return;
  }

  if (action?.type === 'back') {
    if (window.history.length > 1) {
      window.history.back();
      return;
    }

    window.location.assign(action.fallbackTo || '/');
  }
};

const ErrorAction = ({ action, variant = 'primary' }) => {
  if (!action) return null;

  const Icon = actionIcons[action.icon] || actionIcons.home;
  const className = `${actionBaseClass} ${actionVariants[variant]}`;

  if (action.type === 'link' && action.to) {
    return (
      <Link to={action.to} className={className}>
        <Icon size={17} aria-hidden="true" />
        {action.label}
      </Link>
    );
  }

  return (
    <button type="button" className={className} onClick={() => runClientAction(action)}>
      <Icon size={17} aria-hidden="true" />
      {action.label}
    </button>
  );
};

const SystemErrorPage = ({
  statusCode = 500,
  eyebrow = 'Thông báo',
  title,
  message,
  details,
  primaryAction,
  secondaryAction,
  fullScreen = true,
  className = '',
}) => {
  const content = getSystemErrorContent(statusCode);
  const displayTitle = title || content.title;
  const displayMessage = message || content.message;
  const displayPrimaryAction = primaryAction === undefined ? content.primaryAction : primaryAction;
  const displaySecondaryAction =
    secondaryAction === undefined ? content.secondaryAction : secondaryAction;
  const styles = toneStyles[content.tone] || toneStyles.red;
  const Icon = statusIcons[content.icon] || AlertTriangle;

  return (
    <main
      role="alert"
      className={`flex items-center justify-center bg-slate-50 px-4 py-10 text-slate-900 dark:bg-slate-950 dark:text-white ${
        fullScreen ? 'min-h-screen' : 'min-h-64 rounded-lg'
      } ${className}`}
    >
      <section
        className={`w-full max-w-xl rounded-lg border bg-white p-6 text-center shadow-xl shadow-slate-200/50 dark:bg-slate-900 dark:shadow-none sm:p-8 ${styles.border}`}
      >
        <div
          className={`mx-auto flex h-14 w-14 items-center justify-center rounded-lg ring-1 ${styles.icon}`}
        >
          <Icon size={30} aria-hidden="true" />
        </div>

        <p className={`mt-6 text-xs font-bold uppercase tracking-[0.2em] ${styles.eyebrow}`}>
          {eyebrow}
        </p>
        <p className={`mt-3 break-words text-6xl font-black leading-none ${styles.code}`}>
          {content.statusCode}
        </p>
        <h1 className="mt-4 text-2xl font-black text-slate-950 dark:text-white sm:text-3xl">
          {displayTitle}
        </h1>
        <p className="mx-auto mt-3 max-w-md whitespace-pre-line break-words text-sm leading-6 text-slate-600 dark:text-slate-300">
          {displayMessage}
        </p>

        {details && (
          <p className="mx-auto mt-4 max-w-md whitespace-pre-line break-words rounded-lg border border-slate-200 bg-slate-50 p-3 text-left text-xs leading-5 text-slate-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-400">
            {details}
          </p>
        )}

        {(displayPrimaryAction || displaySecondaryAction) && (
          <div className="mt-7 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <ErrorAction action={displayPrimaryAction} />
            <ErrorAction action={displaySecondaryAction} variant="secondary" />
          </div>
        )}
      </section>
    </main>
  );
};

export default SystemErrorPage;
