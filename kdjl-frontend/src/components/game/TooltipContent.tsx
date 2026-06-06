import { PROPS_COLORS, SLOT_NAMES, TIP_COLORS } from './shopConstants';
import { resolveEffect, parseRequires, getTradeStatus } from './shopUtils';
import styles from './TooltipContent.module.css';

type TooltipItem = { name?: string; [key: string]: any };

interface TooltipContentProps {
  item: TooltipItem;
}

/** Parse holeInfo like "ac:100,mc:50" into gem effect descriptions */
function parseHoleInfo(holeInfo?: string, plusnum?: number): string[] {
  if (!plusnum) return ['无卡槽'];
  if (!holeInfo) return [`卡槽数：0/${plusnum}`];

  const holes = holeInfo.split(',').filter(Boolean);
  const lines = [`卡槽数：${holes.length}/${plusnum}`];
  const GEM_LABELS: Record<string, string> = {
    ac: '增加攻击', mc: '增加防御', hp: '增加HP上限', mp: '增加MP上限',
    speed: '增加速度', hits: '增加命中', miss: '增加闪避',
    sdmp: '以MP抵消伤害', szmp: '伤害转化为MP',
    hitshp: '命中吸血', hitsmp: '命中吸蓝', dxsh: '伤害抵销', shjs: '伤害增加', crit: '暴击率增加',
  };
  for (const h of holes) {
    const [k, v] = h.split(':');
    const label = GEM_LABELS[k] ?? k;
    lines.push(`宝石效果：${label}${v}`);
  }
  return lines;
}

/** Parse series like "套装名|id1|id2|id3" and serieseffect like "ac,100,mc,50" */
function parseSeries(series?: string, serieseffect?: string): string[] {
  if (!series) return [];
  const parts = series.split('|');
  const name = parts[0];
  const lines = [`${name}(0/${parts.length - 1})`];

  if (serieseffect) {
    const effects = serieseffect.split(',').filter(Boolean);
    for (let i = 0; i < effects.length; i += 2) {
      const k = effects[i];
      const v = effects[i + 1];
      if (k && v) {
        const label = resolveEffect(`${k},${v}`);
        if (label) lines.push(`(${i / 2 + 1})套装：+${label}`);
      }
    }
  }
  return lines;
}

/** Calculate expiration display string */
function getExpireStr(stime?: number, expire?: string): string | null {
  if (!expire) return null;
  const expireNum = parseInt(expire);
  if (expireNum === 0) return '永久';
  if (!stime || expireNum <= 0) return null;
  const end = stime + expireNum;
  const now = Math.floor(Date.now() / 1000);
  if (end <= now) return '过期';
  const d = new Date(end * 1000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `到期时间:${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function TooltipContent({ item }: TooltipContentProps) {
  const propsColor = item.propsColor ?? 1;
  const nameColor = PROPS_COLORS[propsColor] ?? TIP_COLORS.base;
  const effect = (item as any).effectDesc || resolveEffect(item.effect);
  const pluseffect = (item as any).pluseffectDesc || resolveEffect((item as any).pluseffect);
  const req = parseRequires((item as any).requiresDesc || item.requires);
  const slotName = item.postion != null ? SLOT_NAMES[item.postion] : null;
  const vn = item.varyname;
  const tradeStatus = getTradeStatus(item as any);
  const expireStr = getExpireStr(item.stime, item.expire);

  return (
    <>
      {vn === 9 ? (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
          <div style={{ color: TIP_COLORS.gray }}>{tradeStatus}</div>
          {slotName && <div style={{ color: TIP_COLORS.base }}>{slotName}装备 ({((item as any).plusflag ?? 0) === 1 ? '可强化' : '不可强化'})</div>}
          {effect && <div style={{ color: TIP_COLORS.base }}>{effect}</div>}
          {req && (req.wx || req.lv) && (
            <>
              {req.wx && <div style={{ color: TIP_COLORS.base }}>五行需求：{req.wx}系</div>}
              {req.lv && <div style={{ color: TIP_COLORS.base }}>需求等级：{req.lv}级</div>}
            </>
          )}
          {pluseffect && <div style={{ color: TIP_COLORS.plus }}>{pluseffect}</div>}
          {parseHoleInfo(item.holeInfo, item.plusnum).map((line, i) => (
            <div key={i} style={{ color: i === 0 ? TIP_COLORS.base : '#FF4444' }}>{line}</div>
          ))}
          {parseSeries(item.series, item.serieseffect).map((line, i) => (
            <div key={i} style={{ color: i === 0 ? '#FED625' : '#A8A7A4' }}>{line}</div>
          ))}
          {(item as any).usages && <div style={{ color: TIP_COLORS.base }}>{(item as any).usages}</div>}
        </>
      ) : (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
          <div style={{ color: TIP_COLORS.gray }}>{tradeStatus}</div>
          {effect && <div style={{ color: TIP_COLORS.base }}>{effect}</div>}
          {req && (req.wx || req.lv) && (
            <>
              {req.wx && <div style={{ color: TIP_COLORS.base }}>五行需求：{req.wx}系</div>}
              {req.lv && <div style={{ color: TIP_COLORS.base }}>需求等级：{req.lv}级</div>}
            </>
          )}
          {(item as any).usages && <div style={{ color: TIP_COLORS.base }}>{(item as any).usages}</div>}
        </>
      )}
      {expireStr && <div style={{ color: TIP_COLORS.base }}>{expireStr}</div>}
    </>
  );
}
