import styles from './ResourceBar.module.css';

interface ResourceItem {
  icon: string;
  label: string;
  value: number | string;
}

interface ResourceBarProps {
  items: ResourceItem[];
  className?: string;
}

export default function ResourceBar({ items, className }: ResourceBarProps) {
  return (
    <div className={`${styles.bar} ${className ?? ''}`}>
      {items.map((item, i) => (
        <div key={i} className={styles.row}>
          <img src={item.icon} alt="" />
          <span>{item.label}：{item.value}</span>
        </div>
      ))}
    </div>
  );
}
