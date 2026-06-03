/** Shared constants for all shop/depot/bag panels */

export const CATEGORIES: { label: string; vary: number[] }[] = [
  { label: '全部道具', vary: [] },
  { label: '辅助道具', vary: [1] },
  { label: '增益道具', vary: [2] },
  { label: '捕捉道具', vary: [3] },
  { label: '收集道具', vary: [4] },
  { label: '技能书',   vary: [5] },
  { label: '卡片道具', vary: [6] },
  { label: '进化道具', vary: [7] },
  { label: '合体道具', vary: [8] },
  { label: '装备道具', vary: [9] },
  { label: '精练道具', vary: [10] },
  { label: '宝箱道具', vary: [11] },
  { label: '特殊道具', vary: [12] },
  { label: '功能道具', vary: [13] },
  { label: '宠物卵',   vary: [14] },
  { label: '合成道具', vary: [15] },
];

/** Props color ID → CSS color */
export const PROPS_COLORS: Record<number, string> = {
  1: '#FEFDFA', 2: '#0067CB', 3: '#9833DC', 4: '#14FD10', 5: '#FED625', 6: '#ED9037',
};

/** Equipment slot DB postion → name */
export const SLOT_NAMES = ['翅膀','头部','身体','脚部','武器','项链','戒指','翅膀','手镯','宝石','道具','特殊'];

/** Effect key → display label */
export const ATTR_KEYS: Record<string, string> = {
  ac:'攻击', mc:'防御', hp:'生命', mp:'魔法', speed:'速度', hits:'命中', miss:'闪避',
  addmoney:'获得金币', time:'时间', acrate:'攻击%', mcrate:'防御%', hprate:'生命%',
  mprate:'魔法%', speedrate:'速度%', hitsrate:'命中%', missrate:'闪避%',
  hitshp:'吸血%', hitsmp:'吸蓝%', dxsh:'多行伤害', shjs:'伤害减少', szmp:'数值魔法', sdmp:'速度魔法', crit:'暴击率',
  srchp:'生命上限', srcmp:'魔法上限', addhp:'生命', addmp:'魔法',
};

/** wx element index → name */
export const WX_NAMES = ['所有','金','木','水','火','土','神','神圣'];

/** Tooltip border colors */
export const TIP_COLORS = {
  base: '#FEFDFA',
  plus: '#0067CB',
  gray: '#A8A7A4',
};
