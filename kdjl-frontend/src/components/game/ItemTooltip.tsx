import TooltipContent from './TooltipContent';
import styles from './ItemTooltip.module.css';

type TooltipItem = { name?: string; [key: string]: any };

interface ItemTooltipProps {
  item: TooltipItem;
  x: number;
  y: number;
}

const tipImg = (name: string) => `/images/ui/tips/border4_${name}.gif`;

export default function ItemTooltip({ item, x, y }: ItemTooltipProps) {
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
          <td className={styles.tipBorderTd}><TooltipContent item={item} /></td>
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
