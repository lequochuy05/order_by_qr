import { Link } from 'react-router-dom';
import { LogIn, QrCode, ScanLine, UtensilsCrossed } from 'lucide-react';

const LandingPage = () => (
  <div className="relative min-h-screen overflow-hidden bg-[#231712] text-[#F7F0E7]">
    {/* Ambient Glows */}
    <div className="pointer-events-none absolute -top-40 -right-40 h-96 w-96 rounded-full bg-[#C17C4A]/20 blur-3xl" />
    <div className="pointer-events-none absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-[#56372B]/30 blur-3xl" />

    <div className="relative mx-auto flex min-h-screen w-full max-w-md flex-col px-6 py-8">
      {/* Top Bar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#C17C4A] text-white shadow-md shadow-[#C17C4A]/30">
            <UtensilsCrossed size={20} />
          </div>
          <span className="text-xl font-black tracking-tight text-white">QROS</span>
        </div>
        <Link
          to="/login"
          className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/10 bg-white/5 text-white transition-all hover:bg-white/10 hover:scale-105 active:scale-95"
          aria-label="Đăng nhập"
          title="Đăng nhập"
        >
          <LogIn size={18} />
        </Link>
      </div>

      {/* Main Hero QR Prompt */}
      <div className="flex flex-1 flex-col items-center justify-center text-center">
        <div className="relative mb-8 flex h-40 w-40 items-center justify-center rounded-3xl border border-white/15 bg-white p-4 shadow-2xl shadow-black/50">
          <QrCode className="text-[#231712]" size={110} strokeWidth={1.75} />
          <div className="absolute -right-3 -top-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#C17C4A] text-white shadow-lg shadow-[#C17C4A]/40 animate-pulse">
            <ScanLine size={24} />
          </div>
        </div>
        <h1 className="text-3xl font-black leading-tight tracking-tight text-white">
          Quét mã QR trên bàn
        </h1>
        <p className="mt-3 max-w-xs text-sm font-medium leading-relaxed text-[#D8CCC4]">
          Mở camera điện thoại quét mã QR tại bàn để khám phá thực đơn và đặt món nhanh chóng.
        </p>
      </div>

      {/* Footer Info */}
      <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-center text-xs font-semibold leading-5 text-[#D8CCC4]">
        Nếu bạn là nhân viên hoặc quản lý, hãy bấm vào nút đăng nhập ở góc trên để vào hệ thống vận
        hành.
      </div>
    </div>
  </div>
);

export default LandingPage;
