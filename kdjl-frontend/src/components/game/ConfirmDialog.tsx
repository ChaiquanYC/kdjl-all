import { useState, useRef, useEffect, useCallback, type ReactNode } from 'react';
import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  open: boolean;
  title?: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmDialog({
  open, title = '确认操作', message,
  confirmLabel = '确定', cancelLabel = '取消',
  onConfirm, onCancel,
}: ConfirmDialogProps) {
  const [pos, setPos] = useState({ x: 0, y: 0 });
  const [initPos, setInitPos] = useState(false);
  const dragging = useRef(false);
  const offset = useRef({ x: 0, y: 0 });
  const dialogRef = useRef<HTMLDivElement>(null);

  // Center dialog on first open
  useEffect(() => {
    if (open && !initPos) {
      const cw = 320, ch = 200;
      setPos({ x: (window.innerWidth - cw) / 2, y: (window.innerHeight - ch) / 2 });
      setInitPos(true);
    }
  }, [open, initPos]);

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    dragging.current = true;
    offset.current = { x: e.clientX - pos.x, y: e.clientY - pos.y };
    e.preventDefault();
  }, [pos]);

  useEffect(() => {
    if (!open) return;
    const onMouseMove = (e: MouseEvent) => {
      if (!dragging.current) return;
      setPos({ x: e.clientX - offset.current.x, y: e.clientY - offset.current.y });
    };
    const onMouseUp = () => { dragging.current = false; };
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
    };
  }, [open]);

  if (!open) return null;

  return (
    <div className={styles.overlay}>
      <div ref={dialogRef} className={styles.dialog} style={{ left: pos.x, top: pos.y }}>
        <div className={styles.titleBar} onMouseDown={onMouseDown}>
          <span>{title}</span>
          <button className={styles.closeBtn} onClick={onCancel}>×</button>
        </div>
        <div className={styles.body}>{message}</div>
        <div className={styles.footer}>
          <button className={styles.confirmBtn} onClick={onConfirm}>{confirmLabel}</button>
          <button className={styles.cancelBtn} onClick={onCancel}>{cancelLabel}</button>
        </div>
      </div>
    </div>
  );
}
