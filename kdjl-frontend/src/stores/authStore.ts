import { create } from 'zustand';
import type { Player } from '@/types';
import { apiGet, apiPost } from '@/api/client';
import { connectWs, disconnectWs } from '@/api/websocket';

interface AuthState {
  player: Player | null;
  token: string | null;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  fetchPlayer: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  player: null,
  token: localStorage.getItem('token'),
  loading: false,
  error: null,

  login: async (username: string, password: string) => {
    set({ loading: true, error: null });
    try {
      const res = await apiPost<{ token: string; uid: number; username: string; nickname: string }>(
        '/auth/login',
        { username, password },
      );
      if (res.code === 0 && res.data) {
        localStorage.setItem('token', res.data.token);
        try { connectWs(res.data.token); } catch { /* ws optional */ }
        set({
          token: res.data.token,
          player: {
            id: res.data.uid,
            username: res.data.username,
            nickname: res.data.nickname,
            money: 0, yb: 0, vip: 0, score: 0, prestige: 0,
            inMap: 0, openMap: '', fightTop: 0, maxBag: 30, sex: '',
            onlineTime: 0, newGuideStep: 0,
          } as Player,
          loading: false,
        });
      } else {
        set({ error: res.message || '登录失败', loading: false });
      }
    } catch {
      set({ error: '网络错误', loading: false });
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    disconnectWs();
    set({ player: null, token: null });
  },

  fetchPlayer: async () => {
    const token = get().token;
    if (!token) return;
    try {
      const res = await apiGet<Player>('/player/me');
      if (res.code === 0 && res.data) {
        set({ player: res.data });
      }
    } catch {
      // keep existing player data on error
    }
  },
}));
