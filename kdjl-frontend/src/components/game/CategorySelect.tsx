import { CATEGORIES } from './shopConstants';
import styles from './CategorySelect.module.css';

interface CategorySelectProps {
  value: number;
  onChange: (value: number) => void;
  className?: string;
  showLabel?: boolean;
}

export default function CategorySelect({ value, onChange, className, showLabel }: CategorySelectProps) {
  return (
    <span className={`${styles.wrapper} ${className ?? ''}`}>
      {showLabel && <span className={styles.label}>分类查看</span>}
      <select className={styles.select} value={value} onChange={e => onChange(Number(e.target.value))}>
        {CATEGORIES.map((c, i) => <option key={i} value={i}>{c.label}</option>)}
      </select>
    </span>
  );
}
