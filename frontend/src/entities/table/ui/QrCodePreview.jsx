import { QRCodeCanvas } from 'qrcode.react';

const QrCodePreview = ({ value, size = 128 }) => {
  if (!value) return null;

  return (
    <div className="inline-flex rounded-lg border border-slate-200 bg-white p-3">
      <QRCodeCanvas value={value} size={size} />
    </div>
  );
};

export default QrCodePreview;
