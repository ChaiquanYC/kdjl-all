import { useEffect, useState, useMemo } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './TaskPanel.module.css';

interface KillProgress {
  [monsterList: string]: number;
}

interface TaskInfo {
  id: number; title: string; fromnpc: string; frommsg: string;
  okneed: string; result: string; limitlv: string;
  okneedDesc?: string; resultDesc?: string; limitlvDesc?: string;
  accepted: boolean; acceptId?: number; state?: string;
  killProgress?: KillProgress;
  canComplete?: boolean;
  color?: number; xulie?: number; oknpc?: number;
}

const COLOR_LABELS: Record<number, string> = {
  0: '普通', 1: '主线', 2: '支线', 3: '进化', 4: '活动', 5: '冰滩',
  6: '副本', 7: '钥匙', 8: '宠物', 9: '兑换', 10: 'VIP', 11: '结婚',
};

const NPC_NAMES: Record<string, string> = {
  '1': '仓库管理员', '2': '宠物神殿', '3': '神秘商人', '4': '牧场管理员',
  '5': '道具商人', '6': '玛亚公主', '7': '拍卖管理员', '8': '公告使者', '9': '铁匠',
  '10': '酒馆·索菲雅', '11': '酒馆·凯尔', '12': '酒馆·龙吉', '13': '酒馆·艾利克',
};

function getNpcName(npcIds: string): string {
  if (!npcIds || npcIds === '0') return '任意NPC';
  const ids = npcIds.split('|');
  return ids.map(id => NPC_NAMES[id] || `NPC#${id}`).join('/');
}

function getOkNpcName(npcId: number | undefined): string {
  if (!npcId || npcId === 0) return '任意NPC';
  return NPC_NAMES[String(npcId)] || `NPC#${npcId}`;
}

function formatKillBar(current: number, needed: number): string {
  const pct = Math.min(100, Math.round((current / needed) * 100));
  const filled = Math.round(pct / 5);
  return '█'.repeat(filled) + '░'.repeat(20 - filled) + ` ${current}/${needed}`;
}

function getColorLabel(color?: number): string {
  return COLOR_LABELS[color ?? 0] || '普通';
}

type TabMode = 'available' | 'accepted';

export default function TaskPanel() {
  const [tasks, setTasks] = useState<TaskInfo[]>([]);
  const [acceptedTasks, setAcceptedTasks] = useState<TaskInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedTask, setSelectedTask] = useState<TaskInfo | null>(null);
  const [activeColor, setActiveColor] = useState<number | null>(null);
  const [tab, setTab] = useState<TabMode>('available');
  const closePanel = useGameStore((s) => s.setActivePanel);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);

  const fetchTasks = async () => {
    try {
      const [availRes, acceptedRes] = await Promise.all([
        apiGet<TaskInfo[]>('/tasks'),
        apiGet<TaskInfo[]>('/tasks/accepted'),
      ]);
      if (availRes.code === 0 && availRes.data) setTasks(availRes.data);
      if (acceptedRes.code === 0 && acceptedRes.data) setAcceptedTasks(acceptedRes.data);
    } catch { /* ignore */ }
    setLoading(false);
  };

  useEffect(() => { fetchTasks(); }, []);

  const showMsg = (text: string) => {
    setMsg(text);
    setTimeout(() => setMsg(null), 2500);
  };

  const handleAccept = async (taskId: number) => {
    try {
      const res: any = await apiPost('/tasks/accept/' + taskId, {});
      if (res.code === 0) {
        showMsg('已接受任务');
        await fetchTasks();
        triggerRefresh();
        // Update selectedTask so the button switches from "接受任务" to "提交完成"
        setSelectedTask(prev => {
          if (prev && prev.id === taskId) return { ...prev, accepted: true };
          return prev;
        });
      } else {
        showMsg(res.message || '接受失败');
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message;
      showMsg(msg || '请求失败，请重试');
    }
  };

  const handleComplete = async (taskId: number) => {
    try {
      const res: any = await apiPost('/tasks/complete/' + taskId, {});
      if (res.code === 0) {
        showMsg('任务完成！' + (res.data?.reward || ''));
        await fetchTasks();
        triggerRefresh();
        setSelectedTask(null);
      } else {
        showMsg(res.message || '无法完成');
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message;
      showMsg(msg || '请求失败，请重试');
    }
  };

  const handleAbandon = async (taskId: number) => {
    try {
      const res: any = await apiPost('/tasks/abandon/' + taskId, {});
      if (res.code === 0) {
        showMsg('已放弃任务');
        await fetchTasks();
        setSelectedTask(null);
      } else {
        showMsg(res.message || '放弃失败');
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message;
      showMsg(msg || '请求失败，请重试');
    }
  };

  // Color category counts for available tasks
  const colorCounts = useMemo(() => {
    const counts: Record<number, { total: number; accepted: number }> = {};
    for (const t of tasks) {
      const c = t.color ?? 0;
      if (!counts[c]) counts[c] = { total: 0, accepted: 0 };
      counts[c].total++;
      if (t.accepted) counts[c].accepted++;
    }
    return counts;
  }, [tasks]);

  const availableColors = useMemo(
    () => Object.keys(colorCounts).map(Number).sort((a, b) => a - b),
    [colorCounts]
  );

  const displayTasks = useMemo(() => {
    if (tab === 'accepted') return acceptedTasks;
    if (activeColor != null) return tasks.filter(t => (t.color ?? 0) === activeColor);
    return [];
  }, [tab, activeColor, tasks, acceptedTasks]);

  const handleCatClick = (c: number) => {
    if (activeColor === c && tab === 'available') {
      setActiveColor(null);
      setSelectedTask(null);
    } else {
      setTab('available');
      setActiveColor(c);
      setSelectedTask(null);
    }
  };

  const handleTabSwitch = (newTab: TabMode) => {
    setTab(newTab);
    setActiveColor(null);
    setSelectedTask(null);
  };

  const selectTask = (t: TaskInfo) => {
    setSelectedTask(selectedTask?.id === t.id ? null : t);
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}
      <button className={styles.closeBtn} onClick={() => closePanel(null)} />

      {/* Left nav */}
      <div className={styles.leftNav}>
        {/* Tab switcher */}
        <div style={{ display: 'flex', height: 29, padding: '0 15px', gap: 8, alignItems: 'center' }}>
          <a onClick={() => handleTabSwitch('available')}
            style={{ cursor: 'pointer', color: tab === 'available' ? '#ff4200' : '#915d0c',
              fontWeight: tab === 'available' ? 'bold' : 'normal', fontSize: 13 }}>
            可接任务
          </a>
          <a onClick={() => handleTabSwitch('accepted')}
            style={{ cursor: 'pointer', color: tab === 'accepted' ? '#ff4200' : '#915d0c',
              fontWeight: tab === 'accepted' ? 'bold' : 'normal', fontSize: 13 }}>
            已接任务 ({acceptedTasks.length})
          </a>
        </div>

        <div className={styles.taskList}>
          {tab === 'available' ? (
            <ul className={styles.catList}>
              {availableColors.map((c) => {
                const info = colorCounts[c];
                const expanded = activeColor === c;
                return (
                  <li key={c}>
                    <div className={`${styles.catItem} ${expanded ? styles.catItemOn : ''}`}>
                      <a onClick={() => handleCatClick(c)}>
                        <p>
                          {getColorLabel(c)}
                          {info.accepted > 0 && <em className={styles.catBadge}>{info.accepted}</em>}
                          <small className={styles.catTotal}>({info.total})</small>
                        </p>
                      </a>
                    </div>
                    {expanded && (
                      <ul className={styles.subList}>
                        {displayTasks.length === 0 ? (
                          <li className={styles.subEmpty}><p>暂无任务</p></li>
                        ) : (
                          displayTasks.map((t) => {
                            const isSel = selectedTask?.id === t.id;
                            const cls = t.accepted ? styles.subOk : styles.subCan;
                            return (
                              <li key={t.id}
                                className={`${cls} ${isSel ? styles.subFocus : ''}`}
                                onClick={() => selectTask(t)}>
                                <p>
                                  {t.title}
                                  {t.accepted && t.killProgress && (
                                    <span className={styles.killMini}>
                                      {Object.entries(t.killProgress)
                                        .filter(([k]) => !k.endsWith('_needed'))
                                        .reduce((sum, [, v]) => sum + (v as number), 0)}
                                    </span>
                                  )}
                                </p>
                              </li>
                            );
                          })
                        )}
                      </ul>
                    )}
                  </li>
                );
              })}
            </ul>
          ) : (
            <ul className={styles.catList}>
              {acceptedTasks.length === 0 ? (
                <li className={styles.subEmpty}><p>暂无已接任务</p></li>
              ) : (
                acceptedTasks.map((t) => {
                  const isSel = selectedTask?.id === t.id;
                  const cls = t.canComplete ? styles.subOk : styles.subCan;
                  return (
                    <li key={t.id}
                      className={`${cls} ${isSel ? styles.subFocus : ''}`}
                      onClick={() => selectTask(t)}>
                      <p>
                        {t.title}
                        {t.canComplete && <em className={styles.catBadge}>可交</em>}
                        {t.killProgress && (
                          <span className={styles.killMini}>
                            {Object.entries(t.killProgress)
                              .filter(([k]) => !k.endsWith('_needed'))
                              .reduce((sum, [, v]) => sum + (v as number), 0)}
                          </span>
                        )}
                      </p>
                    </li>
                  );
                })
              )}
            </ul>
          )}
        </div>
      </div>

      {/* Right — task detail */}
      <div className={styles.rightCont}>
        {selectedTask ? (
          <div className={styles.taskInfo}>
            <h2>{selectedTask.title}</h2>
            <div className={styles.infoBody}>
              <p className={styles.infoMeta}>
                NPC: {getNpcName(selectedTask.fromnpc)}
                {selectedTask.oknpc && (
                  <span> → 完成: {getOkNpcName(selectedTask.oknpc)}</span>
                )}
                <span className={styles.infoColor} style={{ background: getColorBg(selectedTask.color) }}>
                  {getColorLabel(selectedTask.color)}
                </span>
              </p>

              {selectedTask.frommsg && selectedTask.frommsg !== '0' && (
                <div className={styles.infoDesc} dangerouslySetInnerHTML={{
                  __html: selectedTask.frommsg.replace(/<br\s*\/?>/gi, '<br/>')
                }} />
              )}

              <p className={styles.infoLabel}>任务条件：</p>
              <p className={styles.infoText}>{selectedTask.okneedDesc || '无'}</p>

              {selectedTask.accepted && selectedTask.killProgress && (
                <div className={styles.killBars}>
                  {Object.entries(selectedTask.killProgress)
                    .filter(([k]) => !k.endsWith('_needed'))
                    .map(([monsterList, current]) => {
                      const needed = (selectedTask.killProgress![monsterList + '_needed'] as number) || 1;
                      return (
                        <p key={monsterList} className={styles.killRow}>
                          击杀进度：<span className={styles.killBar}>{formatKillBar(current as number, needed)}</span>
                        </p>
                      );
                    })}
                </div>
              )}

              <p className={styles.infoLabel}>任务奖励：</p>
              <p className={styles.infoReward}>{selectedTask.resultDesc || '无'}</p>

              {selectedTask.limitlv && selectedTask.limitlv !== '0' && selectedTask.limitlvDesc && (
                <p className={styles.infoLimit}>限制：{selectedTask.limitlvDesc}</p>
              )}

              <div className={styles.infoBtns}>
                {!selectedTask.accepted ? (
                  <input type="button" className={styles.btnAccept} value="接受任务"
                    onClick={() => handleAccept(selectedTask.id)} />
                ) : (
                  <>
                    <input type="button" className={styles.btnComplete} value="提交完成"
                      onClick={() => handleComplete(selectedTask.id)} />
                    <input type="button" className={styles.btnAbandon} value="放弃任务"
                      onClick={() => handleAbandon(selectedTask.id)} />
                  </>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className={styles.noSelect}>
            <h2>
              {tab === 'accepted' ? '已接任务' :
                activeColor != null ? getColorLabel(activeColor) + '任务' : '任务详情'}
            </h2>
            <div className={styles.noSelectBody}>
              <p className={styles.noSelectIcon}>📋</p>
              <p>← 点击左侧任务查看详情</p>
              {tab === 'accepted' && (
                <p className={styles.noSelectCount}>
                  {acceptedTasks.filter(t => t.canComplete).length}个可提交 / {acceptedTasks.length}个进行中
                </p>
              )}
              {tab === 'available' && activeColor != null && (
                <p className={styles.noSelectCount}>
                  {displayTasks.filter(t => t.accepted).length}个进行中 / {displayTasks.length}个任务
                </p>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function getColorBg(color?: number): string {
  const map: Record<number, string> = {
    0: '#636e72', 1: '#d63031', 2: '#0984e3', 3: '#00b894',
    4: '#e17055', 5: '#74b9ff', 6: '#a29bfe', 7: '#fdcb6e',
    8: '#fd79a8', 9: '#e84393', 10: '#ffeaa7', 11: '#fab1a0',
  };
  return map[color ?? 0] || '#636e72';
}
