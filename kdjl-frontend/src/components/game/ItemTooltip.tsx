import { PROPS_COLORS, SLOT_NAMES, TIP_COLORS } from './shopConstants';
import { resolveEffect, parseRequires } from './shopUtils';
import styles from './ItemTooltip.module.css';

type TooltipItem = { name?: string; [key: string]: any };

interface ItemTooltipProps {
  item: TooltipItem;
  x: number;
  y: number;
}

const tipImg = (name: string) => `/images/ui/tips/border4_${name}.gif`;

export default function ItemTooltip({ item, x, y }: ItemTooltipProps) {
  const propsColor = item.propsColor ?? 1;
  const nameColor = PROPS_COLORS[propsColor] ?? TIP_COLORS.base;
  const effect = (item as any).effectDesc || resolveEffect(item.effect);
  const pluseffect = (item as any).pluseffectDesc || resolveEffect((item as any).pluseffect);
  const req = parseRequires((item as any).requiresDesc || item.requires);
  const slotName = item.postion != null ? SLOT_NAMES[item.postion] : null;
  const sell = item.sell ?? item.buy;
  const vn = item.varyname;

  const content = (
    <>
      {vn === 9 ? (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
          <div style={{ color: TIP_COLORS.gray }}>可交易</div>
          {slotName && <div style={{ color: TIP_COLORS.base }}>{slotName}装备 ({((item as any).plusflag ?? 0) === 1 ? '可强化' : '不可强化'})</div>}
          {effect && <div style={{ color: TIP_COLORS.base }}>{effect}</div>}
          {req && (req.wx || req.lv) && (
            <>
              {req.wx && <div style={{ color: TIP_COLORS.base }}>五行需求：{req.wx}系</div>}
              {req.lv && <div style={{ color: TIP_COLORS.base }}>需求等级：{req.lv}级</div>}
            </>
          )}
          {pluseffect && <div style={{ color: TIP_COLORS.plus }}>{pluseffect}</div>}
          {(item as any).usages && <div style={{ color: TIP_COLORS.base }}>{(item as any).usages}</div>}
        </>
      ) : (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
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
      {sell != null && <div style={{ color: TIP_COLORS.base }}>售价：{sell}金</div>}
      {(item as any).expire && <div style={{ color: TIP_COLORS.base }}>{(item as any).expire}</div>}
    </>
  );

  return (
    <table className={styles.tooltip} style={{ left: x + 12, top: Math.max(0, y - 160) }} cellPadding={0} cellSpacing={0} border={0}>
      <tbody>
        <tr>
          <td className={styles.tipCorner}><img src={tipImg('tl')} alt="" /></td>
          <td className={styles.tipEdge} style={{ backgroundImage: `url(${tipImg('t')})` }} />
          <td className={styles.tipCorner}><img src={tipImg('tr')} alt="" /></td>
        </tr>
        <tr>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('l')})` }} />
          <td className={styles.tipBorderTd} />
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('r')})` }} />
        </tr>
        <tr>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('l')})` }} />
          <td className={styles.tipBorderTd}>{content}</td>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('r')})` }} />
        </tr>
        <tr>
          <td className={styles.tipCorner}><img src={tipImg('bl')} alt="" /></td>
          <td className={styles.tipEdge} style={{ backgroundImage: `url(${tipImg('b')})` }} />
          <td className={styles.tipCorner}><img src={tipImg('br')} alt="" /></td>
        </tr>
      </tbody>
    </table>
  );
}

/** Hook-style tooltip props for hover behavior */
export function useTooltipProps<T>(setTooltip: React.Dispatch<React.SetStateAction<{ item: T; x: number; y: number } | null>>, item: T) {
  return {
    onMouseEnter: (e: React.MouseEvent) => setTooltip({ item, x: e.clientX, y: e.clientY }),
    onMouseMove: (e: React.MouseEvent) => setTooltip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null),
    onMouseLeave: () => setTooltip(null),
  };
}
