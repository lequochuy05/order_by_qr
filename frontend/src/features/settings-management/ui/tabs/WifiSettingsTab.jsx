import { QrCode, Wifi } from 'lucide-react';
import { QRCodeCanvas } from 'qrcode.react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const WifiSettingsTab = ({
  settings,
  setSettings,
  saving,
  saveDisabled,
  onSave,
  wifiQrValue,
  copy,
}) => (
  <section className="min-w-0">
    <SectionHeader icon={Wifi} title={copy.wifi.title} subtitle={copy.wifi.subtitle} />
    <div className="grid min-w-0 gap-6 xl:grid-cols-[minmax(0,1fr)_300px]">
      <SettingsGroup title={copy.wifi.detailsTitle}>
        <div className="grid gap-5">
          <TextField
            label={copy.wifi.ssid}
            description={copy.wifi.ssidHelp}
            value={settings.wifiName}
            onChange={(value) => setSettingsValue(setSettings, 'wifiName', value)}
          />
          <TextField
            label={copy.wifi.password}
            description={copy.wifi.passwordHelp}
            value={settings.wifiPassword}
            onChange={(value) => setSettingsValue(setSettings, 'wifiPassword', value)}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.wifi.previewTitle}>
        <div className="flex min-h-64 flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 p-5 transition-colors dark:border-slate-700 dark:bg-slate-950">
          {wifiQrValue ? (
            <>
              <QRCodeCanvas value={wifiQrValue} size={190} includeMargin />
              <p className="mt-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400">
                {copy.wifi.qrLabel}
              </p>
            </>
          ) : (
            <div className="flex h-48 flex-col items-center justify-center text-slate-400 dark:text-slate-500">
              <QrCode size={44} />
              <p className="mt-3 text-sm font-semibold">{copy.wifi.empty}</p>
            </div>
          )}
        </div>
      </SettingsGroup>
    </div>
    <SaveButton
      saving={saving}
      disabled={saveDisabled}
      onClick={onSave}
      label={copy.actions.save}
      className="mt-6"
    />
  </section>
);

export default WifiSettingsTab;
