import React from 'react';

const AUTH_RAIN_STREAKS = [
  { id: 1, left: '3%', width: 1.5, height: 48, duration: '2.4s', delay: '-0.3s', opacity: 0.4 },
  { id: 2, left: '9%', width: 2, height: 65, duration: '1.9s', delay: '-1.1s', opacity: 0.55 },
  { id: 3, left: '16%', width: 1, height: 38, duration: '2.8s', delay: '-0.7s', opacity: 0.35 },
  { id: 4, left: '24%', width: 2, height: 70, duration: '2.0s', delay: '-1.8s', opacity: 0.5 },
  { id: 5, left: '33%', width: 1.5, height: 50, duration: '2.5s', delay: '-0.5s', opacity: 0.45 },
  { id: 6, left: '42%', width: 2.5, height: 80, duration: '1.7s', delay: '-1.4s', opacity: 0.65 },
  { id: 7, left: '51%', width: 1, height: 40, duration: '2.9s', delay: '-0.2s', opacity: 0.35 },
  { id: 8, left: '60%', width: 2, height: 60, duration: '2.1s', delay: '-1.6s', opacity: 0.5 },
  { id: 9, left: '69%', width: 1.5, height: 45, duration: '2.6s', delay: '-0.9s', opacity: 0.45 },
  { id: 10, left: '78%', width: 2, height: 75, duration: '1.8s', delay: '-1.3s', opacity: 0.6 },
  { id: 11, left: '87%', width: 1, height: 36, duration: '3.0s', delay: '-0.6s', opacity: 0.35 },
  { id: 12, left: '94%', width: 2, height: 55, duration: '2.2s', delay: '-1.9s', opacity: 0.5 },
];

export const AuthRainBackground = ({ children }) => (
  <div className="relative flex min-h-screen items-center justify-center bg-slate-50 p-4 transition-colors dark:bg-[#0B0F17] overflow-hidden">
    {/* Decorative Diagonal Rain Animation */}
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
      {/* Ambient Gradient Glows */}
      <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-indigo-500/15 blur-3xl dark:bg-indigo-500/10" />
      <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-violet-500/15 blur-3xl dark:bg-violet-500/10" />

      {/* Rain Streaks */}
      {AUTH_RAIN_STREAKS.map((streak) => (
        <span
          key={streak.id}
          className="auth-rain-streak"
          style={{
            left: streak.left,
            width: `${streak.width}px`,
            height: `${streak.height}px`,
            animationDuration: streak.duration,
            animationDelay: streak.delay,
            '--rain-opacity': streak.opacity,
          }}
        />
      ))}
    </div>

    {/* Content Container */}
    <div className="relative z-10 w-full max-w-md">{children}</div>
  </div>
);

export default AuthRainBackground;
