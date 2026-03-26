import React, { createContext, useState, useContext, useEffect, useRef, useCallback } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const timerRef = useRef(null);

  const logout = useCallback(() => {
    localStorage.clear();
    setUser(null);
  }, []);

  const resetTimer = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    // 20 phút = 20 * 60 * 1000 = 1200000 ms
    timerRef.current = setTimeout(() => {
      logout();
      // Chỉ điều hướng về /login nếu đang ở trong trang quản lý (/admin)
      if (window.location.pathname.startsWith('/admin')) {
        window.location.href = '/login';
      }
    }, 1200000);
  }, [logout]);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const role = localStorage.getItem('role');
    const fullName = localStorage.getItem('fullname');
    const avatarUrl = localStorage.getItem('avatarUrl');
    const userId = localStorage.getItem('userId');

    if (token) {
      setUser({ role, fullName, avatarUrl, userId: userId ? Number(userId) : null });
    }
    setLoading(false);
  }, []);

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

    events.forEach(event => window.addEventListener(event, handleActivity));

    resetTimer(); // Bắt đầu đếm giờ ngay khi đăng nhập xong

    return () => {
      events.forEach(event => window.removeEventListener(event, handleActivity));
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [user, resetTimer]);

  const login = (data) => {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('role', data.role);
    localStorage.setItem('fullname', data.fullName);
    localStorage.setItem('userId', data.userId);
    if (data.avatarUrl) localStorage.setItem('avatarUrl', data.avatarUrl);
    setUser({ role: data.role, fullName: data.fullName, avatarUrl: data.avatarUrl, userId: data.userId });
  };

  const updateUser = (updatedFields) => {
    setUser(prev => {
      const newUser = { ...prev, ...updatedFields };
      if (updatedFields.fullName) localStorage.setItem('fullname', updatedFields.fullName);
      if (updatedFields.avatarUrl !== undefined) localStorage.setItem('avatarUrl', updatedFields.avatarUrl || '');
      if (updatedFields.role) localStorage.setItem('role', updatedFields.role);
      return newUser;
    });
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, updateUser }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);