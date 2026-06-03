import type { ReactNode } from 'react';
import styles from './ShopFooter.module.css';

interface ShopFooterProps {
  count: number;
  onCountChange: (count: number) => void;
  children?: ReactNode;
  className?: string;
}

/** Shared footer with count input. Pass buy/sell buttons as children. */
export default function ShopFooter({ count, onCountChange, children, className }: ShopFooterProps) {
  return (
    <div className={`${styles.footer} ${className ?? ''}`}>
      数量：<input className={styles.numInput} type="text" value={count}
        onChange={e => onCountChange(Number(e.target.value) || 1)} />
      {children}
    </div>
  );
}

interface ShopFooterButtonProps {
  onClick?: () => void;
  disabled?: boolean;
  children: ReactNode;
}

export function FooterBtn({ onClick, disabled, children }: ShopFooterButtonProps) {
  return (
    <button className={styles.btn} disabled={disabled} onClick={onClick}>{children}</button>
  );
}
