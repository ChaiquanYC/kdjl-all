import { Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useOnlineHeartbeat } from '@/hooks/useOnlineHeartbeat';
import GameLayout from '@/components/layout/GameLayout';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';

// Decode JWT payload without verifying signature
function decodeJwt(token: string): { exp?: number } | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(payload);
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);
  const hydrate = useAuthStore((s) => s.hydrate);
  const logout = useAuthStore((s) => s.logout);
  const [validating, setValidating] = useState(!hydrated);

  useEffect(() => {
    if (!hydrated) {
      hydrate().then((ok) => {
        setValidating(false);
        if (!ok) logout();
      });
    }
  }, []);

  if (!token) return <Navigate to="/login" replace />;

  // Check if token is expired
  const jwt = decodeJwt(token);
  if (!jwt || !jwt.exp) {
    logout();
    return <Navigate to="/login" replace />;
  }

  if (jwt.exp * 1000 < Date.now()) {
    logout();
    return <Navigate to="/login" replace />;
  }

  if (validating) {
    return <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', color: '#888' }}>验证登录...</div>;
  }

  return <>{children}</>;
}

function GameContent() {
  useOnlineHeartbeat();
  return <GameLayout />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <GameContent />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
