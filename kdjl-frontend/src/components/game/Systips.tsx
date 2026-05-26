import { useSystipsStore } from '@/stores/systipsStore';
import styles from './Systips.module.css';

export default function Systips() {
  const message = useSystipsStore((s) => s.message);
  if (!message) return null;
  return <div className={styles.toast}>{message}</div>;
}
