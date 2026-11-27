import React, {
  createContext,
  useState,
  useContext,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from 'react';
import { authService } from '../api/authService.js';
import {
  getAccessToken,
  registerAuthFailureListener,
  setAccessToken,
} from '@shared/api/httpClient.js';
import { queryClient } from '@shared/api/queryClient.js';
import { adminWsService } from '@shared/lib/websocket.js';
import { isAdminRoute, isKitchenDisplayRoute } from '@shared/lib/routeMatchers.js';

const AuthContext = createContext();
const INACTIVITY_TIMEOUT_MS = 20 * 60 * 1000;
const ACTIVITY_THROTTLE_MS = 15_000;

const toAuthUser = (data) => ({
  role: data.role,
  fullName: data.fullName,
  avatarUrl: data.avatarUrl,
  userId: data.userId,
  email: data.email,
  mustChangePassword: Boolean(data.mustChangePassword),
});

const reconnectAdminWebSocketIfNeeded = () => {
  if (isAdminRoute(window.location.pathname)) {
    adminWsService.reconnect();
  } else {
    adminWsService.disconnect();
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  const [loading, setLoading] = useState(() => isAdminRoute(window.location.pathname));
  const timerRef = useRef(null);
  const lastActivityResetRef = useRef(0);

  const logout = useCallback(() => {
    const activeToken = getAccessToken();
    adminWsService.disconnect();
    setAccessToken(null);
    queryClient.clear();
    authService.logout(activeToken).catch(() => {});
    setUser(null);
  }, []);

  useEffect(() => {
    return registerAuthFailureListener(() => {
      adminWsService.disconnect();
      setAccessToken(null);
      queryClient.clear();
      setUser(null);
      if (isAdminRoute(window.location.pathname)) {
        window.location.href = '/login';
      }
    });
  }, []);

  useEffect(() => {
    // Chỉ chặn render để silent-refresh trên các route quản trị (/admin/*)
    // Trang chủ (/), /login, /forgot-password, /reset-password và menu QR render ngay lập tức.
    if (!isAdminRoute(window.location.pathname)) {
      return;
    }

    let mounted = true;
    authService
      .refresh()
      .then((data) => {
        if (!mounted) return;
        setAccessToken(data.accessToken);
        setUser(toAuthUser(data));
        reconnectAdminWebSocketIfNeeded();
      })
      .catch(() => {
        setAccessToken(null);
        adminWsService.disconnect();
        if (mounted) setUser(null);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, []);

  const resetTimer = useCallback(() => {
    const scheduleInactivityCheck = () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        // Màn hình KDS (/admin/kitchen) là màn treo tường hands-free: miễn tự động đăng xuất
        if (isKitchenDisplayRoute(window.location.pathname)) {
          scheduleInactivityCheck();
          return;
        }
        logout();
        if (isAdminRoute(window.location.pathname)) {
          window.location.href = '/login';
        }
      }, INACTIVITY_TIMEOUT_MS);
    };
    scheduleInactivityCheck();
  }, [logout]);

  // Khởi tạo bộ đếm theo dõi hoạt động người dùng (throttle 15s + passive listeners)
  useEffect(() => {
    if (!user) {
      if (timerRef.current) clearTimeout(timerRef.current);
      return;
    }

    const events = ['mousemove', 'mousedown', 'click', 'scroll', 'keypress', 'touchstart'];
    const listenerOptions = { passive: true };

    const handleActivity = () => {
      const now = Date.now();
      if (now - lastActivityResetRef.current < ACTIVITY_THROTTLE_MS) {
        return;
      }
      lastActivityResetRef.current = now;
      resetTimer();
    };

    events.forEach((event) => window.addEventListener(event, handleActivity, listenerOptions));

    lastActivityResetRef.current = Date.now();
    resetTimer(); // Bắt đầu đếm giờ ngay khi đăng nhập xong

    return () => {
      events.forEach((event) => window.removeEventListener(event, handleActivity, listenerOptions));
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [user, resetTimer]);

  const login = useCallback((data) => {
    setAccessToken(data.accessToken);
    setUser(toAuthUser(data));
    reconnectAdminWebSocketIfNeeded();
  }, []);

  const updateUser = useCallback((updatedFields) => {
    setUser((prev) => {
      const newUser = { ...prev, ...updatedFields };
      return newUser;
    });
  }, []);

  const contextValue = useMemo(
    () => ({ user, login, logout, loading, updateUser }),
    [user, login, logout, loading, updateUser],
  );

  return (
    <AuthContext.Provider value={contextValue}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);
