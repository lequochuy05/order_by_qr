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
          <label className="grid gap-2 rounded-2xl border border-slate-200/80 bg-white p-4 transition-all hover:border-indigo-200 dark:border-slate-800/80 dark:bg-slate-900/60 dark:hover:border-indigo-500/30">
            <span className="text-sm font-bold text-slate-800 dark:text-slate-100">
              {copy.bill.paperSize}
            </span>
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              {copy.bill.paperSizeHelp}
            </span>
            <select
              value={settings.billPaperSize}
              onChange={(event) =>
                setSettingsValue(setSettings, 'billPaperSize', event.target.value)
              }
              className="h-11 rounded-xl border border-slate-200/80 bg-white px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:focus:ring-indigo-500/20"
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
