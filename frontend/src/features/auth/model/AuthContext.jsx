import React, { createContext, useState, useContext, useEffect, useRef, useCallback } from 'react';
import { authService } from '@features/auth/api/authService.js';
import { setAccessToken } from '@shared/api/httpClient.js';
import { queryClient } from '@shared/api/queryClient.js';
import { adminWsService } from '@shared/lib/websocket.js';
import { isAdminRoute, isCustomerMenuPath } from '@shared/lib/routeMatchers.js';

const AuthContext = createContext();

const toAuthUser = (data) => ({
  role: data.role,
  fullName: data.fullName,
  avatarUrl: data.avatarUrl,
  userId: data.userId,
  email: data.email,
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

  const [loading, setLoading] = useState(() => !isCustomerMenuPath(window.location.pathname));
  const timerRef = useRef(null);

  const logout = useCallback(() => {
    adminWsService.disconnect();
    setAccessToken(null);
    queryClient.clear();
    authService.logout().catch(() => {});
    setUser(null);
  }, []);

  useEffect(() => {
    // Không tự động refresh ở trang khách hàng (Menu)
    if (isCustomerMenuPath(window.location.pathname)) {
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
    if (timerRef.current) clearTimeout(timerRef.current);
    // 20 phút = 20 * 60 * 1000 = 1200000 ms
    timerRef.current = setTimeout(() => {
      logout();
      // Chỉ điều hướng về /login nếu đang ở trong trang quản lý (/admin)
      if (isAdminRoute(window.location.pathname)) {
        window.location.href = '/login';
      }
    }, 1200000); // 20 phút = 20 * 60 * 1000
  }, [logout]);

  // Khởi tạo bộ đếm theo dõi hoạt động người dùng
  useEffect(() => {
    if (!user) {
      if (timerRef.current) clearTimeout(timerRef.current);
      return;
    }

    const events = ['mousemove', 'mousedown', 'click', 'scroll', 'keypress', 'touchstart'];

    const handleActivity = () => {
      resetTimer();
    };

    events.forEach((event) => window.addEventListener(event, handleActivity));

    resetTimer(); // Bắt đầu đếm giờ ngay khi đăng nhập xong

    return () => {
      events.forEach((event) => window.removeEventListener(event, handleActivity));
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [user, resetTimer]);

  const login = (data) => {
    setAccessToken(data.accessToken);
    setUser(toAuthUser(data));
    reconnectAdminWebSocketIfNeeded();
  };

  const updateUser = useCallback((updatedFields) => {
    setUser((prev) => {
      const newUser = { ...prev, ...updatedFields };
      return newUser;
    });
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, updateUser }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);
