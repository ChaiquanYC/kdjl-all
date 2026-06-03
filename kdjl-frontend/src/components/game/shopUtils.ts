import { ATTR_KEYS, WX_NAMES } from './shopConstants';

/** Filter items by category label */
export function filterByCat<T extends { category?: string }>(items: T[], cat: number, categories: { label: string }[]) {
  if (cat === 0) return items;
  const label = categories[cat]?.label ?? '';
  return items.filter(i => (i.category ?? '') === label);
}

/** Parse effect string like "ac,100|mc,50" into readable text */
export function resolveEffect(effect?: string): string {
  if (!effect) return '';
  return effect.split('|').map(p => {
    const [k, v] = p.split(',');
    if (!k || !v) return '';
    const label = ATTR_KEYS[k] || k;
    if (k.endsWith('rate') || k === 'hitshp' || k === 'hitsmp' || k === 'crit' || k === 'dxsh')
      return `+${v}% ${label}`;
    return `+${v} ${label}`;
  }).filter(Boolean).join(' ');
}

/** Parse requires string like "lv:10,wx:2" into { lv, wx } */
export function parseRequires(requires?: string) {
  if (!requires) return null;
  const parts = requires.split(',');
  let lv: string | null = null;
  let wx: string | null = null;
  for (const p of parts) {
    const [k, v] = p.split(':');
    if (k === 'lv') lv = v;
    else if (k === 'wx') wx = WX_NAMES[Number(v)] ?? v;
  }
  return { lv, wx };
}

/** Get trade status text for a bag item */
export function getTradeStatus(item: { cantrade?: number; propslock?: number }) {
  if (item.cantrade === 0) return item.propslock === 1 ? '可交易' : '不可交易';
  if (item.cantrade === 1) return '可交易';
  return '不可交易';
}
