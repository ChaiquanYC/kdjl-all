import { create } from 'zustand';

interface SystipsState {
  message: string | null;
  show: (msg: string) => void;
  hide: () => void;
}

export const useSystipsStore = create<SystipsState>((set) => ({
  message: null,
  show: (msg) => set({ message: msg }),
  hide: () => set({ message: null }),
}));

let timer: ReturnType<typeof setTimeout> | null = null;

export function systips(msg: string, duration = 10000) {
  if (timer) clearTimeout(timer);
  useSystipsStore.getState().show(msg);
  timer = setTimeout(() => useSystipsStore.getState().hide(), duration);
}
