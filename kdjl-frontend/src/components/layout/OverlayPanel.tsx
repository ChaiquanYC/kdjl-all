import { useRef, useState, useCallback, useEffect, type ReactNode } from 'react';
import styles from './OverlayPanel.module.css';

interface Props {
  title: string;
  children: ReactNode;
  defaultLeft?: number;
  defaultTop?: number;
  width?: number;
  height?: number;
  onClose: () => void;
}

export default function OverlayPanel({
  children,
  defaultLeft = 200, defaultTop = 30,
  width = 400, height = 350,
  onClose,
}: Props) {
  const [pos, setPos] = useState({ left: defaultLeft, top: defaultTop });
  const dragRef = useRef<{ startX: number; startY: number; startLeft: number; startTop: number } | null>(null);

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    let el = e.target as HTMLElement | null;
    while (el) {
      const tag = el?.tagName;
      if (tag && (tag === 'SELECT' || tag === 'INPUT' || tag === 'BUTTON' || tag === 'TEXTAREA' || tag === 'OPTION')) return;
      if (el?.getAttribute?.('data-drag') === 'false') return;
      if (el?.classList?.contains(styles.body)) break;
      el = el.parentElement;
    }
    dragRef.current = {
      startX: e.clientX, startY: e.clientY,
      startLeft: pos.left, startTop: pos.top,
    };
  }, [pos]);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!dragRef.current) return;
      const dx = e.clientX - dragRef.current.startX;
      const dy = e.clientY - dragRef.current.startY;
      setPos({
        left: Math.max(0, dragRef.current.startLeft + dx),
        top: Math.max(0, dragRef.current.startTop + dy),
      });
    };
    const onUp = () => { dragRef.current = null; };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, []);

  return (
    <div
      className={styles.overlay}
      style={{ left: pos.left, top: pos.top, width, height }}
      onMouseDown={onMouseDown}
    >
      <div className={styles.body} style={{ height: '100%' }}>
        {children}
      </div>
    </div>
  );
}
