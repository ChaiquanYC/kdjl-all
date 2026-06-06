import { useEffect, useRef } from 'react';
import TooltipContent from './TooltipContent';
import styles from './BagTooltip.module.css';

type TooltipItem = { name?: string; [key: string]: any };

interface BagTooltipProps {
  item: TooltipItem | null;
  onTimeout: () => void;
}

const tipImg = (name: string) => `/images/ui/tips/border4_${name}.gif`;

export default function BagTooltip({ item, onTimeout }: BagTooltipProps) {
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (item) {
      timerRef.current = setTimeout(onTimeout, 20000);
      return () => clearTimeout(timerRef.current);
    }
  }, [item, onTimeout]);

  if (!item) return null;

  return (
    <div className={styles.bagTooltip}>
      <table cellPadding={0} cellSpacing={0} border={0}>
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
    </div>
  );
}
