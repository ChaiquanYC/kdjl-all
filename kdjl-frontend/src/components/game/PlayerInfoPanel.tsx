import { useAuthStore } from '@/stores/authStore';
import styles from './PlayerInfoPanel.module.css';

const elementNames = ['', '金', '木', '水', '火', '土', '神', '神圣'];

export default function PlayerInfoPanel() {
  const player = useAuthStore(s => s.player);
  if (!player) return <div className={styles.container}>加载中...</div>;

  const row = (label: string, value: string | number) => (
    <div className={styles.row}>
      <span className={styles.label}>{label}</span>
      <span className={styles.value}>{value}</span>
    </div>
  );

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.avatar}>
          <img src={`/images/ui/head/${player.headImg || 0}.png`} alt="头像"
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
        </div>
        <div className={styles.basic}>
          <div className={styles.nickname}>{player.nickname}</div>
          <div className={styles.username}>@{player.username} (ID: {player.id})</div>
          {player.vip > 0 && <span className={styles.vip}>VIP{player.vip}</span>}
        </div>
      </div>

      <div className={styles.stats}>
        <h3>经济信息</h3>
        {row('金币', player.money.toLocaleString())}
        {row('元宝', player.yb.toLocaleString())}
        {row('水晶', (player.sj || 0).toLocaleString())}
        {row('威望', (player.prestige || 0).toLocaleString())}

        <h3>战斗信息</h3>
        {row('主战宠物ID', player.mbid || '未设置')}
        {row('背包容量', player.maxBag)}
        {row('仓库容量', player.maxMc || 30)}

        <h3>在线信息</h3>
        {row('在线时长(秒)', player.onlineTime || 0)}
        {player.dblExpFlag && player.dblExpFlag > 1 && (
          <div className={styles.expBuff}>
            双倍经验: {['', '1x', '1.5x', '2x', '2.5x', '3x'][player.dblExpFlag]} 倍
          </div>
        )}
      </div>
    </div>
  );
}
