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
  jPrestige?: number;
  activeScore?: number;
  vipLast?: number;
  sj: number;
  paimoney?: number;
  paisj?: number;
  paiyb?: number;
  inMap: number;
  openMap: string;
  fightTop: number;
  maxBag: number;
  sex: string;
  mbid: number;
  fightbb: number;
  merge: number;
  mergeCount?: number;
  maxMc: number;
  headImg: number;
  dblExpFlag?: number;
  dblsTime?: number;
  maxDblExpTime?: number;
  sysAutoSum?: number;
  maxAutoFitSum?: number;
  friendList?: string;
  teamAutoTimes?: number;
  tiaozhan?: number;
  petCount?: number;
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

export interface PropsItem {
  id: number;
  name: string;
  img?: string;
  varyname?: number;
  vary?: number;
  effect?: string;
  requires?: string;
  usages?: string;
  sell?: number;
  buy?: number;
  yb?: number;
  sj?: number;
  postion?: number;
  propscolor?: string;
  propslock?: number;
  prestige?: number;
  pluseffect?: string;
  plusflag?: number;
  pluspid?: number;
  plusget?: string;
  plusnum?: number;
  series?: string;
  serieseffect?: string;
  merge?: number;
  stime?: number;
  endtime?: number;
  effectDesc?: string;
  requiresDesc?: string;
  usagesDesc?: string;
  serieseffectDesc?: string;
  pluseffectDesc?: string;
  category?: string;
}

export interface ChatMessage {
  id: string;
  senderId: number;
  senderName: string;
  content: string;
  channel: 'world' | 'private' | 'guild' | 'team' | 'system';
  timestamp: number;
  vip?: number;          // 0=普通 1=VIP 2=结婚 3=VIP+结婚
  isMarried?: boolean;
}
