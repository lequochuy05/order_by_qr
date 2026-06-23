import { Printer, ReceiptText, Wifi } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const BillSettingsTab = ({ settings, setSettings, saving, saveDisabled, onSave, copy }) => (
  <section className="min-w-0">
    <SectionHeader icon={ReceiptText} title={copy.bill.title} subtitle={copy.bill.subtitle} />

    <div className="grid gap-5">
      <SettingsGroup title={copy.bill.contentTitle}>
        <div className="grid gap-5">
          <TextField
            label={copy.bill.titleLabel}
            description={copy.bill.titleHelp}
            maxLength="100"
            value={settings.billTitle}
            onChange={(value) => setSettingsValue(setSettings, 'billTitle', value)}
          />
          <TextField
            label={copy.bill.footer}
            description={copy.bill.footerHelp}
            maxLength="255"
            value={settings.billFooterMessage}
            onChange={(value) => setSettingsValue(setSettings, 'billFooterMessage', value)}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.bill.behaviorTitle}>
        <div className="grid gap-4">
          <label className="grid gap-2 rounded-lg border border-slate-200 p-4 dark:border-slate-800">
            <span className="text-sm font-bold text-slate-800 dark:text-slate-100">
              {copy.bill.paperSize}
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              {copy.bill.paperSizeHelp}
            </span>
            <select
              value={settings.billPaperSize}
              onChange={(event) =>
                setSettingsValue(setSettings, 'billPaperSize', event.target.value)
              }
              className="h-11 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
            >
              <option value="58">58 mm</option>
              <option value="80">80 mm</option>
            </select>
          </label>
          <ToggleRow
            icon={Wifi}
            title={copy.bill.showWifi}
            description={copy.bill.showWifiDesc}
            checked={settings.showWifiOnBill}
            onChange={(value) => setSettingsValue(setSettings, 'showWifiOnBill', value)}
          />
          <ToggleRow
            icon={Printer}
            title={copy.bill.autoPrint}
            description={copy.bill.autoPrintDesc}
            checked={settings.autoPrintBill}
            onChange={(value) => setSettingsValue(setSettings, 'autoPrintBill', value)}
          />
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

export default BillSettingsTab;
