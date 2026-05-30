import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { apiPost } from '@/api/client';

const HEARTBEAT_INTERVAL = 30_000; // 30 seconds

function sendLogoutBeacon() {
  const token = localStorage.getItem('token');
  if (!token) return;
  try {
    fetch('/api/player/logout', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      keepalive: true,
    });
  } catch { /* best-effort */ }
}

export function useOnlineHeartbeat() {
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    if (!token) return;

    // Immediate heartbeat on mount/login
    apiPost('/player/heartbeat').catch(() => {});

    // Periodic heartbeat
    const interval = setInterval(() => {
      apiPost('/player/heartbeat').catch(() => {});
    }, HEARTBEAT_INTERVAL);

    // Flush on page close/refresh
    const onBeforeUnload = () => sendLogoutBeacon();
    window.addEventListener('beforeunload', onBeforeUnload);

    // Flush accumulated time when tab becomes visible again
    // (browser throttles intervals in background tabs)
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        apiPost('/player/heartbeat').catch(() => {});
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      clearInterval(interval);
      window.removeEventListener('beforeunload', onBeforeUnload);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [token]);
}
