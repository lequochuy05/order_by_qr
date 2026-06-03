import { readAdminPreferences } from '../hooks/useAdminPreferences';

let audioCtx = null;

const getAudioContext = () => {
  if (!audioCtx || audioCtx.state === 'closed') {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
};

/**
 * Short 2-note chime for new orders/updates.
 * Plays only if notificationSound preference is enabled.
 */
export const playNotificationSound = () => {
  const prefs = readAdminPreferences();
  if (!prefs.notificationSound) return;

  try {
    const ctx = getAudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.connect(gain);
    gain.connect(ctx.destination);

    // Note 1: D5
    osc.frequency.setValueAtTime(587.33, ctx.currentTime);
    gain.gain.setValueAtTime(0.12, ctx.currentTime);
    // Note 2: A5
    osc.frequency.setValueAtTime(880.0, ctx.currentTime + 0.15);
    gain.gain.setValueAtTime(0.12, ctx.currentTime + 0.15);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45);

    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + 0.45);
  } catch {
    // Web Audio not available
  }
};

/**
 * Longer, more insistent alert for kitchen mode.
 * Plays only if loudSound preference is enabled.
 */
export const playLoudSound = () => {
  const prefs = readAdminPreferences();
  if (!prefs.loudSound) return;

  try {
    const ctx = getAudioContext();

    const playTone = (freq, startTime, duration) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'square';
      osc.frequency.setValueAtTime(freq, startTime);
      osc.connect(gain);
      gain.connect(ctx.destination);
      gain.gain.setValueAtTime(0.08, startTime);
      gain.gain.exponentialRampToValueAtTime(0.001, startTime + duration);
      osc.start(startTime);
      osc.stop(startTime + duration);
    };

    const now = ctx.currentTime;
    // 3-note urgent pattern repeated twice
    playTone(880, now, 0.15);
    playTone(1047, now + 0.18, 0.15);
    playTone(1319, now + 0.36, 0.2);
    playTone(880, now + 0.65, 0.15);
    playTone(1047, now + 0.83, 0.15);
    playTone(1319, now + 1.01, 0.25);
  } catch {
    // Web Audio not available
  }
};
