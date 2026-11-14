import { useRef, useEffect } from 'react';

/**
 * SpotlightGrid
 * Full-screen interactive dot-matrix spotlight grid background.
 * - Always visible as the base background (z-0) with pointer-events-none.
 * - Captures mouse movements with window capture listeners for 0ms lag tracking.
 * - Spotlight radius is tuned to ~160px (strictly within 100-200px range).
 * - Light mode: Pure white canvas (#FFFFFF) with subtle dots, illuminating into vivid blue (#2563EB) around the cursor.
 * - Dark mode: Midnight canvas (#0B0F17) with subtle dots, illuminating into neon indigo (#818CF8) around the cursor.
 */
const SpotlightGrid = ({ children, className = '' }) => {
  const bgRef = useRef(null);

  useEffect(() => {
    const updateCoords = (clientX, clientY) => {
      const el = bgRef.current;
      if (!el) return;
      el.style.setProperty('--spotlight-x', `${clientX}px`);
      el.style.setProperty('--spotlight-y', `${clientY}px`);
      el.style.setProperty('--spotlight-opacity', '1');
    };

    const handlePointerMove = (e) => {
      updateCoords(e.clientX, e.clientY);
    };

    const handlePointerLeave = () => {
      bgRef.current?.style.setProperty('--spotlight-opacity', '0');
    };

    window.addEventListener('mousemove', handlePointerMove, { passive: true, capture: true });
    window.addEventListener('pointermove', handlePointerMove, { passive: true, capture: true });
    document.addEventListener('mouseleave', handlePointerLeave);

    return () => {
      window.removeEventListener('mousemove', handlePointerMove, { capture: true });
      window.removeEventListener('pointermove', handlePointerMove, { capture: true });
      document.removeEventListener('mouseleave', handlePointerLeave);
    };
  }, []);

  return (
    <>
      {/* Fixed Full-Screen Background Container */}
      <div
        ref={bgRef}
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-0 overflow-hidden bg-white dark:bg-[#0B0F17] transition-colors"
        style={{
          '--spotlight-x': '-500px',
          '--spotlight-y': '-500px',
          '--spotlight-opacity': '0',
        }}
      >
        {/* 1. Base Subtle Dot Grid Layer across the entire screen (Light Mode) */}
        <div
          className="absolute inset-0 dark:hidden"
          style={{
            backgroundImage: 'radial-gradient(circle, #cbd5e1 1.2px, transparent 1.2px)',
            backgroundSize: '24px 24px',
            backgroundPosition: '0 0',
            opacity: 0.65,
          }}
        />

        {/* Base Subtle Dot Grid Layer across the entire screen (Dark Mode) */}
        <div
          className="absolute inset-0 hidden dark:block"
          style={{
            backgroundImage: 'radial-gradient(circle, #334155 1.2px, transparent 1.2px)',
            backgroundSize: '24px 24px',
            backgroundPosition: '0 0',
            opacity: 0.45,
          }}
        />

        {/* 2. Spotlight Dot Grid Layer (Light Mode: Vivid Blue dots within ~160px radius) */}
        <div
          className="absolute inset-0 transition-opacity duration-150 dark:hidden"
          style={{
            opacity: 'var(--spotlight-opacity, 0)',
            backgroundImage: 'radial-gradient(circle, #2563EB 2px, transparent 2px)',
            backgroundSize: '24px 24px',
            backgroundPosition: '0 0',
            WebkitMaskImage:
              'radial-gradient(160px circle at var(--spotlight-x) var(--spotlight-y), black 30%, rgba(0, 0, 0, 0.45) 65%, transparent 100%)',
            maskImage:
              'radial-gradient(160px circle at var(--spotlight-x) var(--spotlight-y), black 30%, rgba(0, 0, 0, 0.45) 65%, transparent 100%)',
          }}
        />

        {/* Spotlight Dot Grid Layer (Dark Mode: Neon Indigo dots within ~160px radius) */}
        <div
          className="absolute inset-0 hidden transition-opacity duration-150 dark:block"
          style={{
            opacity: 'var(--spotlight-opacity, 0)',
            backgroundImage: 'radial-gradient(circle, #818CF8 2px, transparent 2px)',
            backgroundSize: '24px 24px',
            backgroundPosition: '0 0',
            WebkitMaskImage:
              'radial-gradient(160px circle at var(--spotlight-x) var(--spotlight-y), black 30%, rgba(0, 0, 0, 0.45) 65%, transparent 100%)',
            maskImage:
              'radial-gradient(160px circle at var(--spotlight-x) var(--spotlight-y), black 30%, rgba(0, 0, 0, 0.45) 65%, transparent 100%)',
          }}
        />

        {/* 3. Soft Ambient Radial Glow within 180px radius */}
        <div
          className="absolute inset-0 transition-opacity duration-150 dark:hidden"
          style={{
            opacity: 'var(--spotlight-opacity, 0)',
            background:
              'radial-gradient(180px circle at var(--spotlight-x) var(--spotlight-y), rgba(37, 99, 235, 0.14) 0%, rgba(99, 102, 241, 0.05) 50%, transparent 100%)',
          }}
        />
        <div
          className="absolute inset-0 hidden transition-opacity duration-150 dark:block"
          style={{
            opacity: 'var(--spotlight-opacity, 0)',
            background:
              'radial-gradient(180px circle at var(--spotlight-x) var(--spotlight-y), rgba(99, 102, 241, 0.20) 0%, rgba(59, 130, 246, 0.08) 50%, transparent 100%)',
          }}
        />
      </div>

      {/* Render children if provided */}
      {children && <div className={`relative z-10 ${className}`}>{children}</div>}
    </>
  );
};

export default SpotlightGrid;
