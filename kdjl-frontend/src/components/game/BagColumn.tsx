import type { ReactNode } from 'react';
import { useAuthStore } from '@/stores/authStore';
import layoutStyles from './ShopLayout.module.css';
import styles from './BagColumn.module.css';

export interface BagColumnItem {
  id: number; propId: number; count: number; sell: number; zbing?: number;
  name?: string; varyname?: number;
}

interface BagColumnProps {
  items: BagColumnItem[];
  selId: number | null;
  onSelect: (item: BagColumnItem) => void;
  title?: ReactNode;
  extraHeader?: ReactNode;
  listVariant?: 'scroll' | 'fixed';
  listClassName?: string;
  className?: string;
  footer?: ReactNode;
}

const HEADER = (
  <thead><tr>
    <th className={layoutStyles.thIcon}></th>
    <th className={styles.thName}>名称</th>
    <th className={styles.thPrice}>卖价</th>
    <th className={styles.thCount}>数量</th>
  </tr></thead>
);

export default function BagColumn({
  items, selId, onSelect, title, extraHeader, listVariant = 'scroll', listClassName, className, footer,
}: BagColumnProps) {
  const player = useAuthStore(s => s.player);
  const maxBag = player?.maxBag ?? 30;
  const filtered = items.filter(i => i.count > 0 && i.zbing !== 1);

  const tbody = (
    <tbody>
      {filtered.length === 0 ? (
        <tr><td colSpan={4} className={layoutStyles.empty}>背包空空</td></tr>
      ) : filtered.map(item => (
        <tr key={item.id}
          className={`${layoutStyles.row} ${selId === item.id ? layoutStyles.rowSel : ''}`}
          onClick={() => onSelect(item)}>
          <td className={layoutStyles.tdIcon}>
            {item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}
          </td>
          <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
          <td className={styles.tdPrice}>{item.sell ?? 0}</td>
          <td className={styles.tdCount}>{item.count}</td>
        </tr>
      ))}
    </tbody>
  );

  const isFixed = listVariant === 'fixed';

  return (
    <div className={`${layoutStyles.column} ${className ?? ''}`}>
      <div className={styles.colHeader}>
        {title ?? <img src="/images/ui/icon04.jpg" alt="背包物品" />}
        {extraHeader}
      </div>
      <div className={`${isFixed ? layoutStyles.itemListFixed : layoutStyles.itemList} ${listClassName ?? ''}`}>
        {isFixed && <table className={layoutStyles.table}>{HEADER}</table>}
        {isFixed ? (
          <div className={layoutStyles.itemBody}>
            <table className={layoutStyles.table}>{tbody}</table>
          </div>
        ) : (
          <table className={layoutStyles.table}>{HEADER}{tbody}</table>
        )}
      </div>
      {footer ?? (
        <div className={styles.bagFoot}>
          背包空间：{filtered.length}/{maxBag}
        </div>
      )}
    </div>
  );
}
