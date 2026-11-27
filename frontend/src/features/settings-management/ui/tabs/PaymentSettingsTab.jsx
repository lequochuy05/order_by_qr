import { Banknote, CheckCircle2, CreditCard, QrCode, Smartphone } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '@features/settings-management/lib/settingsUtils.js';

const PaymentSettingsTab = ({
  settings,
  setSettings,
  saving,
  saveDisabled,
  onSave,
  copy,
  errors = {},
  onClearError,
}) => (
  <section className="min-w-0">
    <SectionHeader icon={CreditCard} title={copy.payment.title} subtitle={copy.payment.subtitle} />

    <div className="grid gap-5">
      <SettingsGroup title={copy.payment.methodsTitle}>
        <div className="grid gap-4 lg:grid-cols-2">
          <ToggleRow
            icon={Banknote}
            title={copy.payment.cash}
            description={copy.payment.cashDesc}
            checked={settings.cashPaymentEnabled}
            onChange={(value) => setSettingsValue(setSettings, 'cashPaymentEnabled', value)}
          />
          <ToggleRow
            icon={QrCode}
            title={copy.payment.online}
            description={copy.payment.onlineDesc}
            checked={settings.onlinePaymentEnabled}
            onChange={(value) => setSettingsValue(setSettings, 'onlinePaymentEnabled', value)}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.payment.expiryTitle}>
        <TextField
          label={copy.payment.expiry}
          description={copy.payment.expiryHelp}
          type="number"
          min="1"
          max="120"
          value={settings.paymentQrExpiresInMinutes}
          error={errors?.paymentQrExpiresInMinutes}
          onChange={(value) => {
            setSettingsValue(setSettings, 'paymentQrExpiresInMinutes', value);
            onClearError?.('paymentQrExpiresInMinutes');
          }}
          className="max-w-md"
        />
      </SettingsGroup>

      <SettingsGroup title={copy.payment.settlementTitle}>
        <div className="grid gap-4 lg:grid-cols-2">
          <ToggleRow
            icon={CheckCircle2}
            title={copy.payment.allowSettlementWithUnfinishedItems}
            description={copy.payment.allowSettlementWithUnfinishedItemsDesc}
            checked={settings.allowSettlementWithUnfinishedItems}
            onChange={(value) =>
              setSettingsValue(setSettings, 'allowSettlementWithUnfinishedItems', value)
            }
          />
          <ToggleRow
            icon={Smartphone}
            title={copy.payment.customerSelfCheckoutEnabled}
            description={copy.payment.customerSelfCheckoutEnabledDesc}
            checked={settings.customerSelfCheckoutEnabled}
            onChange={(value) =>
              setSettingsValue(setSettings, 'customerSelfCheckoutEnabled', value)
            }
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

export default PaymentSettingsTab;
