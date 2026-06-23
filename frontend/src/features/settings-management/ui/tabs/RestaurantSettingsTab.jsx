import { Building2, Image as ImageIcon, Loader2, Upload } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const RestaurantSettingsTab = ({
  settings,
  setSettings,
  saving,
  saveDisabled,
  onSave,
  copy,
  errors,
  onClearError,
  onUploadLogo,
  uploadingLogo,
}) => (
  <section className="min-w-0">
    <SectionHeader
      icon={Building2}
      title={copy.restaurant.title}
      subtitle={copy.restaurant.subtitle}
    />

    <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
      <div className="grid gap-5">
        <SettingsGroup title={copy.restaurant.identityTitle}>
          <div className="grid gap-5 md:grid-cols-2">
            <TextField
              label={copy.restaurant.name}
              description={copy.restaurant.nameHelp}
              value={settings.restaurantName}
              error={errors.restaurantName}
              onChange={(value) => {
                setSettingsValue(setSettings, 'restaurantName', value);
                onClearError('restaurantName');
              }}
            />
            <TextField
              label={copy.restaurant.hotline}
              description={copy.restaurant.hotlineHelp}
              value={settings.restaurantPhone}
              onChange={(value) => setSettingsValue(setSettings, 'restaurantPhone', value)}
            />
            <TextField
              label={copy.restaurant.email}
              description={copy.restaurant.emailHelp}
              type="email"
              value={settings.restaurantEmail}
              onChange={(value) => setSettingsValue(setSettings, 'restaurantEmail', value)}
            />
            <TextField
              label={copy.restaurant.address}
              description={copy.restaurant.addressHelp}
              value={settings.restaurantAddress}
              onChange={(value) => setSettingsValue(setSettings, 'restaurantAddress', value)}
            />
          </div>
        </SettingsGroup>

        <SettingsGroup title={copy.restaurant.displayTitle}>
          <div className="grid gap-5 md:grid-cols-3">
            <TextField
              label={copy.restaurant.currency}
              description={copy.restaurant.currencyHelp}
              maxLength="10"
              value={settings.currency}
              onChange={(value) => setSettingsValue(setSettings, 'currency', value)}
            />
            <TextField
              label={copy.restaurant.taxPercent}
              description={copy.restaurant.taxPercentHelp}
              type="number"
              min="0"
              max="100"
              step="0.01"
              value={settings.taxPercent}
              onChange={(value) => setSettingsValue(setSettings, 'taxPercent', value)}
            />
            <TextField
              label={copy.restaurant.serviceChargePercent}
              description={copy.restaurant.serviceChargePercentHelp}
              type="number"
              min="0"
              max="100"
              step="0.01"
              value={settings.serviceChargePercent}
              onChange={(value) => setSettingsValue(setSettings, 'serviceChargePercent', value)}
            />
          </div>
        </SettingsGroup>
      </div>

      <SettingsGroup title={copy.restaurant.logoTitle}>
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-950">
          {settings.logoUrl ? (
            <img
              src={settings.logoUrl}
              alt="Restaurant logo"
              className="h-44 w-full object-contain p-4"
            />
          ) : (
            <div className="flex h-44 flex-col items-center justify-center text-slate-400 dark:text-slate-500">
              <ImageIcon size={42} />
              <p className="mt-3 text-sm font-semibold">{copy.restaurant.logoEmpty}</p>
            </div>
          )}
        </div>

        <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-dashed border-orange-300 bg-orange-50 px-4 py-3 text-sm font-bold text-orange-600 transition hover:bg-orange-100 dark:border-orange-500/40 dark:bg-orange-500/10 dark:text-orange-300">
          {uploadingLogo ? <Loader2 className="animate-spin" size={17} /> : <Upload size={17} />}
          {uploadingLogo ? copy.actions.uploading : copy.actions.uploadLogo}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            disabled={uploadingLogo}
            className="sr-only"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) onUploadLogo(file);
              event.target.value = '';
            }}
          />
        </label>
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

export default RestaurantSettingsTab;
