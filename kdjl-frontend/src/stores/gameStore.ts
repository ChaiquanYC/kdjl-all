import { create } from 'zustand';
import { apiGet } from '@/api/client';
import type { Pet, Item, ChatMessage, PropsItem, ApiResponse } from '@/types';

export type Panel = 'pets' | 'bag' | 'equip' | 'tasks' | 'map' | 'city' | 'depot' | 'zb' | 'smshop' | 'guild' | 'shop' | 'rank' | 'gm' | 'team' | 'auction' | 'inherit' | 'marry' | 'friend' | 'zhanbu' | 'sd' | null;
export type GameView = 'map' | 'city' | 'pets' | 'shop' | 'depot' | 'zb' | 'smshop' | 'auction' | 'ranch' | 'pvp' | 'zhanbu' | 'sd' | 'profile' | null;

interface GameState {
  pets: Pet[];
  bag: Item[];
  chatMessages: ChatMessage[];
  currentMapId: number;
  inBattle: boolean;
  activePanel: Panel;
  gameView: GameView;
  battlePet: { id: number; name: string } | null;
  battleMonster: { id: number; name: string } | null;
  battleMapId: number | null;
  battleDifficulty: number;
  refreshTrigger: number;
  selectedPetId: number | null;
  propsMap: Record<number, PropsItem>;
  propsLoaded: boolean;

  setPets: (pets: Pet[]) => void;
  setBag: (bag: Item[]) => void;
  addChatMessage: (msg: ChatMessage) => void;
  setCurrentMap: (mapId: number) => void;
  setInBattle: (inBattle: boolean) => void;
  setActivePanel: (panel: Panel) => void;
  setGameView: (view: GameView) => void;
  startBattle: (petId: number, petName: string, monsterId: number, monsterName: string, mapId?: number) => void;
  endBattle: () => void;
  setBattleDifficulty: (difficulty: number) => void;
  triggerRefresh: () => void;
  setSelectedPetId: (id: number | null) => void;
  loadProps: () => Promise<void>;
}

export const useGameStore = create<GameState>((set, get) => ({
  pets: [],
  bag: [],
  chatMessages: [],
  currentMapId: 1,
  inBattle: false,
  activePanel: null,
  gameView: null,
  battlePet: null,
  battleMonster: null,
  battleMapId: null,
  battleDifficulty: 1,
  refreshTrigger: 0,
  selectedPetId: null,
  propsMap: {},
  propsLoaded: false,

  setPets: (pets) => set({ pets }),
  setBag: (bag) => set({ bag }),
  addChatMessage: (msg) =>
    set((s) => ({ chatMessages: [...s.chatMessages.slice(-199), msg] })),
  setCurrentMap: (currentMapId) => set({ currentMapId }),
  setInBattle: (inBattle) => set({ inBattle }),
  setActivePanel: (activePanel) => set({ activePanel }),
  setGameView: (gameView) => set({ gameView, activePanel: null }),
  startBattle: (petId, petName, monsterId, monsterName, mapId) =>
    set({ inBattle: true, battlePet: { id: petId, name: petName }, battleMonster: { id: monsterId, name: monsterName }, battleMapId: mapId ?? null }),
  endBattle: () =>
    set({ inBattle: false, battlePet: null, battleMonster: null, battleMapId: null, battleDifficulty: 1 }),
  setBattleDifficulty: (battleDifficulty) => set({ battleDifficulty }),
  triggerRefresh: () => set((s) => ({ refreshTrigger: s.refreshTrigger + 1 })),
  setSelectedPetId: (selectedPetId) => set({ selectedPetId }),
  loadProps: async () => {
    if (get().propsLoaded) return;
    try {
      const res = await apiGet<PropsItem[]>('/bag/props/all');
      const data = (res as ApiResponse<PropsItem[]>).data;
      if (data) {
        const map: Record<number, PropsItem> = {};
        for (const p of data) map[p.id] = p;
        set({ propsMap: map, propsLoaded: true });
      }
    } catch (e) {
      console.error('Failed to load props:', e);
    }
  },
}));
