import { Bell, Moon, Sun, Volume2 } from 'lucide-react';

import { SectionHeader, ToggleRow } from '../components/FormControls.jsx';
import { setPreferenceValue } from '../settingsUtils.js';

const PreferencesTab = ({ preferences, setPreferences, copy }) => (
  <section className="min-w-0">
    <SectionHeader
      icon={Bell}
      title={copy.preferences.title}
      subtitle={copy.preferences.subtitle}
    />
    <div className="grid gap-4">
      <ToggleRow
        icon={Volume2}
        title={copy.preferences.sound}
        description={copy.preferences.soundDesc}
        checked={preferences.notificationSound}
        onChange={(value) => setPreferenceValue(setPreferences, 'notificationSound', value)}
      />
      <ToggleRow
        icon={Bell}
        title={copy.preferences.loudSound}
        description={copy.preferences.loudSoundDesc}
        checked={preferences.loudSound}
        onChange={(value) => setPreferenceValue(setPreferences, 'loudSound', value)}
      />
      <ToggleRow
        icon={preferences.darkMode ? Moon : Sun}
        title={copy.preferences.darkMode}
        description={copy.preferences.darkModeDesc}
        checked={preferences.darkMode}
        onChange={(value) => setPreferenceValue(setPreferences, 'darkMode', value)}
      />
      <label className="grid gap-2 rounded-2xl border border-slate-200/80 bg-white p-4 transition-all hover:border-indigo-200 dark:border-slate-800/80 dark:bg-slate-900/60 dark:hover:border-indigo-500/30">
        <span className="text-sm font-bold text-slate-800 dark:text-slate-100">
          {copy.preferences.language}
        </span>
        <select
          value={preferences.language}
          onChange={(event) => setPreferenceValue(setPreferences, 'language', event.target.value)}
          className="h-11 rounded-xl border border-slate-200/80 bg-white px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:focus:ring-indigo-500/20"
        >
          <option value="vi">Tiếng Việt</option>
          <option value="en">English</option>
        </select>
      </label>
    </div>
  </section>
);

export default PreferencesTab;
