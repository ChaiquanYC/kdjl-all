import { useState, useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useGameStore } from '@/stores/gameStore';
import { apiGet } from '@/api/client';
import styles from './PlayerInfoPanel.module.css';

export default function PlayerInfoPanel() {
  const player = useAuthStore(s => s.player);
  const fetchPlayer = useAuthStore(s => s.fetchPlayer);
  const refreshTrigger = useGameStore((s) => s.refreshTrigger);
  const [tab, setTab] = useState(1);
  const [petCount, setPetCount] = useState(0);

  useEffect(() => {
    fetchPlayer();
    apiGet<{ id: number }[]>('/pets').then(r => {
      if (r.code === 0 && r.data) setPetCount(r.data.length);
    });
  }, [refreshTrigger]); return <div className={styles.container}>加载中...</div>;

  const sexLabel = player.sex === '1' ? '男' : player.sex === '2' ? '女' : '保密';
  const fightTop = player.fightTop ? String(player.fightTop).replace(':', ', 败：') : '0, 败：0';
  const mergeName = player.merge > 0 ? `婚配ID:${player.merge}` : '婚姻:未婚';
  const dblFlag = player.dblExpFlag || 0;
  const dblMult = dblFlag === 2 ? 1.5 : dblFlag === 3 ? 2 : dblFlag === 4 ? 2.5 : dblFlag === 5 ? 3 : 1;
  const dblRemaining = Math.max(0, ((player.dblsTime || 0) + (player.maxDblExpTime || 0) - Math.floor(Date.now() / 1000)));
  const tiaozhanLabel = (player.tiaozhan ?? 1) === 1 ? '允许' : '不允许';
  const friendNames = (player.friendList || '').split(',').filter(Boolean);

  return (
    <div className={styles.box}>
      {/* Left: avatar + name — PHP .self_l */}
      <div className={styles.selfL}>
        <div className={styles.selfName}><strong>玩家名称：{player.nickname}</strong></div>
        <div className={styles.selfRole}>
          <img src={`/images/head/3${player.headImg || 0}.gif`} alt="头像"
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
        </div>
      </div>

      {/* Right: tabs + content — PHP .self_r */}
      <div className={styles.selfR}>
        <ul className={styles.selftab}>
          <li onClick={() => setTab(1)} className={tab === 1 ? styles.on : ''}>
            <span className={tab === 1 ? styles.tabActive : styles.tabLabel}>属性</span>
          </li>
          <li onClick={() => setTab(2)} className={tab === 2 ? styles.on : ''}>
            <span className={tab === 2 ? styles.tabActive : styles.tabLabel}>好友</span>
          </li>
        </ul>

        {/* Tab 1: Attributes — PHP #con_self_1 */}
        <div className={styles.selfCont} style={{ display: tab === 1 ? 'block' : 'none' }}>
          <div className={styles.miBox}>
            <ul className={styles.top}>
              <li>玩家昵称：{player.nickname}</li>
              <li>威望：{player.prestige || 0}</li>
              <li>水晶：{player.sj || 0}</li>
              <li>贵族威望：{player.jPrestige || 0}</li>
              <li>性别：{sexLabel}</li>
              <li>宠物：{petCount}</li>
              <li>积分：{player.score || 0}</li>
              <li>胜率：胜：{fightTop}</li>
              <li>当月VIP反馈积分：{player.vip || 0}</li>
              <li>金币：{(player.money || 0).toLocaleString()}</li>
              <li title="(VIP反馈积分可以在酒馆处换取反馈道具)">上月VIP反馈积分：{player.vipLast || 0}</li>
              <li>元宝：{(player.yb || 0).toLocaleString()}</li>
              <li>{mergeName}</li>
              <li>双倍经验剩余时间：{dblRemaining} 秒</li>
              <li>双倍经验倍数：{dblMult}</li>
              <li>组队自动战斗次数：{player.teamAutoTimes || 0} 次</li>
            </ul>
            <ul className={styles.bot}>
              <li className={styles.v}>金币版自动战斗次数：{player.sysAutoSum || 0}</li>
              <li className={styles.v}>元宝版自动战斗次数：{player.maxAutoFitSum || 0}</li>
              <li className={styles.v}>是否允许别人挑战自己：{tiaozhanLabel}</li>
            </ul>
          </div>
        </div>

        {/* Tab 2: Friends — PHP #con_self_2 */}
        <div className={styles.selfCont} style={{ display: tab === 2 ? 'block' : 'none' }}>
          <div className={styles.mfBox}>
            <h2>好友列表：</h2>
            <div className={styles.mfBoxCont}>
              {friendNames.length === 0
                ? '您还未添加任何好友！'
                : friendNames.map((name, i) => (
                    <span key={i} className={styles.friendItem}
                      onClick={() => {
                        const inp = document.getElementById('chatmsg') as HTMLInputElement;
                        if (inp) inp.value = '//' + name + ' ';
                      }}>{name}</span>
                  ))
              }
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
