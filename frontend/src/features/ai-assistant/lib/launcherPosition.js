export const LAUNCHER_STORAGE_KEY = 'customer_ai_chat_launcher_position';

const LAUNCHER_SIZE = 64;
const LAUNCHER_MARGIN = 12;

const readSafeAreaInset = (side) => {
  if (typeof document === 'undefined') return 0;
  const value = getComputedStyle(document.documentElement)
    .getPropertyValue(`--safe-area-inset-${side}`)
    .trim();
  return Number.parseFloat(value) || 0;
};

export const clampLauncherPosition = (position) => {
  if (typeof window === 'undefined') return position;

  const viewport = window.visualViewport;
  const viewportLeft = viewport?.offsetLeft || 0;
  const viewportTop = viewport?.offsetTop || 0;
  const viewportWidth = viewport?.width || window.innerWidth;
  const viewportHeight = viewport?.height || window.innerHeight;
  const minX = viewportLeft + readSafeAreaInset('left') + LAUNCHER_MARGIN;
  const minY = viewportTop + readSafeAreaInset('top') + LAUNCHER_MARGIN;
  const maxX =
    viewportLeft + viewportWidth - readSafeAreaInset('right') - LAUNCHER_SIZE - LAUNCHER_MARGIN;
  const maxY =
    viewportTop + viewportHeight - readSafeAreaInset('bottom') - LAUNCHER_SIZE - LAUNCHER_MARGIN;

  return {
    x: Math.min(Math.max(minX, position.x), Math.max(minX, maxX)),
    y: Math.min(Math.max(minY, position.y), Math.max(minY, maxY)),
  };
};

export const getInitialLauncherPosition = () => {
  if (typeof window === 'undefined') return { x: 0, y: 0 };

  try {
    const saved = JSON.parse(localStorage.getItem(LAUNCHER_STORAGE_KEY));
    if (Number.isFinite(saved?.x) && Number.isFinite(saved?.y)) {
      return clampLauncherPosition(saved);
    }
  } catch {
    // Ignore invalid persisted UI state.
  }

  return clampLauncherPosition({
    x: window.innerWidth - LAUNCHER_SIZE - 20,
    y: window.innerHeight - LAUNCHER_SIZE - 104,
  });
};
