/** Shared types for shop/depot/bag panels */

/** Lightweight bag item from API (UserBag fields only) */
export interface BagItemBase {
  id: number;
  propId: number;
  count: number;
  sell: number;
  zbing?: number;
  vary?: string;
  equipPetId?: number;
  cantrade?: number;
  stime?: number;
  holeInfo?: string;
  plusTimesEffect?: string;
}

/** Bag item merged with props data */
export interface BagItemMerged extends BagItemBase {
  name?: string;
  img?: string;
  varyname?: number;
  category?: string;
  effect?: string;
  effectDesc?: string;
  requires?: string;
  requiresDesc?: string;
  pluseffect?: string;
  pluseffectDesc?: string;
  usages?: string;
  usagesDesc?: string;
  propsColor?: number;
  propslock?: number;
  postion?: number;
  plusflag?: number;
  pluspid?: number;
  plusget?: string;
  plusnum?: number;
  series?: string;
  serieseffect?: string;
  serieseffectDesc?: string;
  prestige?: number;
  expire?: string;
}

/** Shop item from /shop/list API */
export interface ShopItem {
  id: number;
  name: string;
  buy: number;
  yb?: number;
  sj?: number;
  prestige?: number;
  img?: string;
  effect?: string;
  effectDesc?: string;
  varyname?: number;
  category?: string;
  stime?: number;
  timelimit?: number;
  requires?: string;
  requiresDesc?: string;
  propsColor?: number;
  vary?: number;
  postion?: number;
  plusflag?: number;
  pluseffect?: string;
  pluseffectDesc?: string;
  sell?: number;
  usages?: string;
  usagesDesc?: string;
}
