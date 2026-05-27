import type { ReactNode } from 'react';
import styles from './ShopLayout.module.css';

interface ShopLayoutProps {
  leftBg: string;
  onReturn: () => void;
  toast?: string | null;
  topArea?: ReactNode;
  children: ReactNode;
  className?: string;
}

export default function ShopLayout({ leftBg, onReturn, toast, topArea, children, className }: ShopLayoutProps) {
  return (
    <div className={`${styles.container} ${className ?? ''}`}>
      {toast && <div className={styles.toast}>{toast}</div>}
      <div className={styles.leftBg} style={{ backgroundImage: `url('${leftBg}')` }}>
        <div className={styles.returnBtn} onClick={onReturn} />
      </div>
      <div className={styles.rightBg}>
        {topArea}
        <div className={styles.columns}>
          {children}
        </div>
      </div>
    </div>
  );
}
