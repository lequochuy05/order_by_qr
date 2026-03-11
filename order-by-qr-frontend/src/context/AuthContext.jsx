import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const role = localStorage.getItem('role');
    const fullName = localStorage.getItem('fullname');

    if (token) {
      setUser({ role, fullName }); 
    }
    setLoading(false);
  }, []);

  const login = (data) => {
    // Lưu các key đồng bộ với code cũ của bạn
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('role', data.role);
    localStorage.setItem('fullname', data.fullName);
    localStorage.setItem('userId', data.userId);
    setUser({ role: data.role, fullName: data.fullName });
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);