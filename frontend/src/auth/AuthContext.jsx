import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import client from '../api/client';
import { login as loginRequest, logout as logoutRequest, registerStudent } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    client.get('/auth/me').then(({ data }) => setUser(data.user)).catch(() => {}).finally(() => setLoading(false));
  }, []);
  const signIn = async (credentials) => {
    setLoading(true);
    try {
      const response = await loginRequest(credentials);
      setUser(response.user);
    } finally {
      setLoading(false);
    }
  };
  const signUp = async (details) => {
    setLoading(true);
    try {
      const response = await registerStudent(details);
      setUser(response.user);
    } finally {
      setLoading(false);
    }
  };
  const signOut = () => {
    logoutRequest();
    setUser(null);
  };
  const value = useMemo(() => ({ user, loading, signIn, signUp, signOut }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
