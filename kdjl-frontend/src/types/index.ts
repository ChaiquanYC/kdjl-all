export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  total?: number;
  page?: number;
  limit?: number;
}

export interface Player {
  id: number;
  username: string;
  nickname: string;
  vip: number;
  money: number;
  yb: number;
  score: number;
  prestige: number;
  sj: number;
  inMap: number;
  openMap: string;
  fightTop: number;
  maxBag: number;
  sex: string;
  mbid: number;
  fightbb: number;
  sj: number;
  merge: number;
  onlineTime: number;
  maxMc: number;
  headImg: number;
  dblExpFlag?: number;
  newGuideStep: number;
}

export interface Pet {
  id: number;
  name: string;
  level: number;
  hp: number;
  mp: number;
  atk: number;
  def: number;
  speed: number;
  element: '金' | '木' | '水' | '火' | '土';
  quality: number; // 1-6: 白绿蓝紫橙红
  exp: number;
}

export interface Item {
  id: number;
  name: string;
  count: number;
  type: number;
  description: string;
}

export interface ChatMessage {
  id: string;
  senderId: number;
  senderName: string;
  content: string;
  channel: 'world' | 'private' | 'guild' | 'team' | 'system';
  timestamp: number;
}
