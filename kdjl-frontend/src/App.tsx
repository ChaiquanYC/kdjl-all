import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import GameLayout from '@/components/layout/GameLayout';
import LoginPage from '@/pages/LoginPage';

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
  const logout = useAuthStore((s) => s.logout);

  if (!token) return <Navigate to="/login" replace />;

  // Check if token is expired
  const jwt = decodeJwt(token);
  if (!jwt || !jwt.exp) {
    logout();
    return <Navigate to="/login" replace />;
  }

  // exp is in seconds, compare with current time in seconds
  if (jwt.exp * 1000 < Date.now()) {
    logout();
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <GameLayout />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
