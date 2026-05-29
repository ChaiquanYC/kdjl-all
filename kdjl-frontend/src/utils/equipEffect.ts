/**
 * Equipment effect formatting utilities.
 * Maps PHP config.props.php label mappings to frontend display.
 */

// Base effects ($_props['zb'])
const BASE_LABELS: Record<string, string> = {
  ac: '攻击', mc: '防御', hp: '生命', mp: '魔法',
  speed: '速度', hits: '命中', miss: '闪避',
};

// Percentage effects ($_props['fjzb1'])
const RATE_LABELS: Record<string, string> = {
  hprate: '生命', mprate: '魔法', acrate: '攻击',
  mcrate: '防御', hitsrate: '命中', missrate: '闪避',
  speedrate: '速度',
};

// Special effects ($_props['fjzb2'] + extra)
const SPECIAL_LABELS: Record<string, string> = {
  dxsh: '伤害抵消', shjs: '伤害加深', shft: '反弹伤害',
  hitshp: '偷取伤害转化为生命', hitsmp: '偷取伤害转化为魔法',
  sdmp: '伤害以MP抵消', szmp: '伤害转化为MP',
  addmoney: '战斗金币增加', time: '战斗等待时间减少',
  crit: '会心一击率',
};

export interface ParsedEffect {
  label: string;
  value: number;
  isPercent: boolean;
  type: 'base' | 'rate' | 'special';
}

/**
 * Parse an effect string like "ac:10,mc:5,hprate:20" into formatted labels.
 */
export function parseEffects(effectStr: string): ParsedEffect[] {
  if (!effectStr) return [];
  const results: ParsedEffect[] = [];

  effectStr.split(',').forEach((pair) => {
    const [key, valStr] = pair.split(':');
    if (!key || !valStr) return;
    const val = Number(valStr.replace('%', ''));
    if (isNaN(val) || val === 0) return;

    const trimmedKey = key.trim();
    if (BASE_LABELS[trimmedKey]) {
      results.push({ label: BASE_LABELS[trimmedKey], value: val, isPercent: false, type: 'base' });
    } else if (RATE_LABELS[trimmedKey]) {
      results.push({ label: RATE_LABELS[trimmedKey] + '%', value: val, isPercent: true, type: 'rate' });
    } else if (SPECIAL_LABELS[trimmedKey]) {
      results.push({ label: SPECIAL_LABELS[trimmedKey], value: val, isPercent: true, type: 'special' });
    }
  });

  return results;
}

/**
 * Format effect string to human-readable text.
 * E.g. "ac:10,hprate:20" → "+10攻击 +20%生命"
 */
export function formatEffectText(effectStr: string): string {
  return parseEffects(effectStr)
    .map((e) => {
      if (e.type === 'special') {
        return `${e.label}${e.value}%`;
      }
      return `+${e.value}${e.isPercent ? '%' : ''}${e.label}`;
    })
    .join(' ');
}
