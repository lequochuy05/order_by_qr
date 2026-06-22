import { Link } from 'react-router-dom';
import { LogIn, QrCode, ScanLine, Utensils } from 'lucide-react';

const LandingPage = () => (
  <div className="min-h-screen bg-neutral-950 text-white">
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col px-6 py-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-500">
            <Utensils size={20} />
          </div>
          <span className="text-lg font-black">QROS</span>
        </div>
        <Link
          to="/login"
          className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 text-white transition-colors hover:bg-white/15"
          aria-label="Đăng nhập"
          title="Đăng nhập"
        >
          <LogIn size={18} />
        </Link>
      </div>

      <div className="flex flex-1 flex-col items-center justify-center text-center">
        <div className="relative mb-8 flex h-36 w-36 items-center justify-center rounded-[2rem] border border-white/10 bg-white">
          <QrCode className="text-neutral-950" size={92} strokeWidth={1.8} />
          <div className="absolute -right-3 -top-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-orange-500 shadow-lg shadow-orange-500/30">
            <ScanLine size={24} />
          </div>
        </div>
        <h1 className="text-3xl font-black leading-tight">Quét mã QR trên bàn</h1>
        <p className="mt-3 max-w-xs text-sm font-medium leading-6 text-neutral-300">
          Mã QR sẽ mở đúng thực đơn và phiên gọi món của bàn bạn đang ngồi.
        </p>
      </div>

      <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-center text-xs font-semibold leading-5 text-neutral-300">
        Nếu bạn là nhân viên, hãy đăng nhập để vào khu vực vận hành.
      </div>
    </div>
  </div>
);

export default LandingPage;
