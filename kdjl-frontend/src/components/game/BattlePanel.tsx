import { useState, useEffect, useCallback, useRef } from 'react';
import { apiPost, apiGet } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import type { ApiResponse } from '@/types';
import styles from './BattlePanel.module.css';

interface SkillInfo { id: number; name: string; level: number; uhp: number; ump: number; }
interface RoundLog {
  round: number; action: string; petDamage: number; petCrit: boolean; petMiss: boolean;
  petLifeSteal: number; monsterDamage: number; monsterMiss: boolean; monsterDead: boolean; petDead: boolean;
}
interface BattleState {
  sessionId: string; round: number; state: string;
  petHp: number; petMaxHp: number; petMp: number; petMaxMp: number;
  monsterHp: number; monsterMaxHp: number;
  petName: string; monsterName: string; monsterId?: number; petId?: number;
  petNowexp?: number; petLexp?: number;
  petImg?: string; petHeadImg?: string; petImgAck?: string; petImgDie?: string;
  monsterImg?: string; monsterImgAck?: string; monsterImgDie?: string;
  monsterLevel?: number; monsterWx?: number; petLevel?: number;
  skills?: SkillInfo[]; message?: string; phase?: string;
  log?: RoundLog;
  won?: boolean; expGained?: number; moneyGained?: number;
  drops?: { propId: number; name: string; count: number }[];
  levelUp?: boolean; newLevel?: number;
  captureSuccess?: boolean; capturedPetId?: number;
}
interface Props { petId: number; monsterId: number; mapId?: number; mapImg?: string; onClose: () => void; onContinue?: () => void; }

type SubPanel = 'skills' | 'capture' | 'items' | 'autoskill' | 'autofight' | null;
type AnimPhase = 'idle' | 'petHit' | 'petDying' | 'monsterHit' | 'monsterDying';

export default function BattlePanel({ petId, monsterId, mapId, mapImg, onClose, onContinue }: Props) {

  const bgUrl = mapId && mapImg
    ? `url('/images/map/t${mapId}/${mapId}.${mapImg}')`
    : `url('/images/ui/battle_bg.jpg')`;
  const [state, setState] = useState<BattleState | null>(null);
  const [loading, setLoading] = useState(true);
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [finished, setFinished] = useState(false);
  const [phase, setPhase] = useState<'waiting' | 'pet' | 'monster'>('waiting');
  const [countdown, setCountdown] = useState(10);
  const [msg, setMsg] = useState<string | null>(null);
  const [subPanel, setSubPanel] = useState<SubPanel>(null);

  // Auto-fight state
  const [autoFight, setAutoFight] = useState(false);
  const [autoMode, setAutoMode] = useState<'gold' | 'yb' | null>(null);
  const [autoSkillId, setAutoSkillId] = useState<number | null>(null);

  const handleAutoFight = (mode: string) => {
    setSubPanel(null);
    apiPost<{ message: string; autoFight: boolean; mode: string; remaining: number; expMult: number }>('/battle/auto-fight', { mode }).then((res: any) => {
      if (res.code === 0 && res.data) {
        setMsg(res.data.message);
        setTimeout(() => setMsg(null), 3000);
        if (res.data.autoFight) {
          setAutoFight(true);
          setAutoMode(res.data.mode);
          // PHP autoFitStart: fire first attack immediately
          if (autoSkillId) doAction('skill', autoSkillId);
          else doAction('attack');
        } else {
          setAutoFight(false);
          setAutoMode(null);
        }
      }
    });
  };

  // Battle items (HP/MP recovery)
  const [bagItems, setBagItems] = useState<{ id: number; name: string; effect: string; propId: number; sums: number }[]>([]);

  // Animation state
  const [animPhase, setAnimPhase] = useState<AnimPhase>('idle');
  const [damageText, setDamageText] = useState<{ text: string; pos: 'right' | 'left' } | null>(null);
  const [petImgSrc, setPetImgSrc] = useState<string | null>(null);
  const [monsterImgSrc, setMonsterImgSrc] = useState<string | null>(null);

  // Battle cooldown — 10s from battle START time (PHP ftime+10)
  const [battleStartTime, setBattleStartTime] = useState(Date.now());
  const [showCooldown, setShowCooldown] = useState(false);
  const [cooldownSec, setCooldownSec] = useState(0);

  const setInBattle = useGameStore((s) => s.setInBattle);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const battleDifficulty = useGameStore((s) => s.battleDifficulty);
  const animTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearAnimTimer = () => { if (animTimer.current) { clearTimeout(animTimer.current); animTimer.current = null; } };

  useEffect(() => { return () => clearAnimTimer(); }, []);

  // Countdown timer — PHP waittime: 10s normal, 3s YB auto, 4s gold auto
  useEffect(() => {
    if (finished || phase !== 'waiting') return;
    let start = autoMode === 'yb' ? 3 : autoMode === 'gold' ? 4 : 10;
    setCountdown(start);
    const timer = setInterval(() => setCountdown(c => {
      if (c <= 1) { clearInterval(timer); return 0; }
      return c - 1;
    }), 1000);
    return () => clearInterval(timer);
  }, [phase, finished]);

  // PHP loadtime: countdown at 0 always auto-attacks (with configured skill)
  // Only fire when countdown actively reaches 0 (not when phase resets with stale 0)
  const cdFired = useRef(false);
  useEffect(() => {
    if (finished || phase !== 'waiting' || countdown !== 0 || acting || cdFired.current) return;
    cdFired.current = true;
    if (autoSkillId) doAction('skill', autoSkillId);
    else doAction('attack');
  }, [countdown, finished, phase, acting]);

  // Reset cdFired when countdown restarts (>0)
  useEffect(() => { if (countdown > 0) cdFired.current = false; }, [countdown]);

  // Auto-continue after victory when auto-fight is on (PHP auto() after 1s)
  useEffect(() => {
    if (!autoFight || !finished || !state?.won) return;
    const t = setTimeout(() => {
      if (onContinue) onContinue();
    }, 1500);
    return () => clearTimeout(t);
  }, [finished, autoFight, state?.won]);

  useEffect(() => {
    setInBattle(true);
    setBattleStartTime(Date.now());
    apiPost<BattleState>('/battle/init', { petId, monsterId, difficulty: battleDifficulty }).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) {
        const d = res.data;
        setState({ ...d, petId, monsterId });
        setPetImgSrc(d.petImg ? `/images/bb/${d.petImg}` : null);
        setMonsterImgSrc(d.monsterImg ? `/images/gpc/${d.monsterImg}` : null);
      } else setError(res.message || '初始化失败');
      setLoading(false);
    }).catch(() => { setError('网络错误'); setLoading(false); });
    return () => setInBattle(false);
  }, [petId, monsterId, setInBattle]);

  const handleBattleResponse = useCallback((s: BattleState) => {
    setState(prev => ({ ...prev, ...s })); // preserve skills/petId/monsterId from init
    if (s.message) { setMsg(s.message); setTimeout(() => setMsg(null), 3000); }
    if (s.state === 'WON' || s.state === 'LOST') {
      setFinished(true);
      if (s.won) triggerRefresh();
    }
  }, [triggerRefresh]);

  // Cooldown countdown — matches PHP loadtime/pause
  useEffect(() => {
    if (cooldownSec <= 0) return;
    const t = setInterval(() => setCooldownSec(c => {
      if (c <= 1) { clearInterval(t); return 0; }
      return c - 1;
    }), 1000);
    return () => clearInterval(t);
  }, [cooldownSec]);

  // When cooldown reaches 0, auto-continue (matches PHP pause(0))
  useEffect(() => {
    if (showCooldown && cooldownSec === 0 && onContinue) {
      onContinue();
      setShowCooldown(false);
    }
  }, [showCooldown, cooldownSec, onContinue]);

  const doUseItem = (bagId: number) => {
    if (acting || finished) return;
    setSubPanel(null);
    setActing(true);
    apiPost<BattleState>('/battle/use-item', { bagId, sessionId: state?.sessionId }).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) {
        handleBattleResponse(res.data);
      } else if (res.message) {
        setMsg(res.message); setTimeout(() => setMsg(null), 2500);
      }
      setActing(false);
    }).catch(() => setActing(false));
  };

  // Capture items (varyname=3) from bag — PHP catcharr
  const [catchItems, setCatchItems] = useState<{ id: number; name: string; effect: string; sums: number }[]>([]);

  const openCapturePanel = () => {
    if (subPanel === 'capture') { setSubPanel(null); return; }
    apiGet<{ id: number; name: string; effect: string; varyname: number; sums: number }[]>('/bag').then((res) => {
      if (res.code === 0 && res.data) {
        const balls = res.data.filter((item: any) => item.varyname === 3);
        setCatchItems(balls);
        setSubPanel('capture');
      } else {
        setSubPanel('capture');
      }
    }).catch(() => setSubPanel('capture'));
  };

  const openItemsPanel = () => {
    if (subPanel === 'items') { setSubPanel(null); return; }
    apiGet<{ id: number; name: string; effect: string; propId: number; sums: number }[]>('/bag').then((res) => {
      if (res.code === 0 && res.data) {
        // Filter for healing items (effect contains hp or mp)
        const healing = res.data.filter((item: any) => {
          const eff = item.effect || '';
          return eff.includes('hp') || eff.includes('mp');
        });
        setBagItems(healing);
      }
      setSubPanel('items');
    }).catch(() => setSubPanel('items'));
  };

  const handleContinue = () => {
    if (!onContinue) { onClose(); return; }
    const elapsed = (Date.now() - battleStartTime) / 1000;
    if (elapsed < 10) {
      const remaining = Math.ceil(10 - elapsed);
      setCooldownSec(remaining);
      setShowCooldown(true);
    } else {
      onContinue();
    }
  };

  // Extract readable skill name from log action ("attack"→"普通攻击", "skill:火球术"→"火球术")
  const skillLabel = (action: string) => {
    if (!action || action === 'attack') return '普通攻击';
    if (action.startsWith('skill:')) return action.substring(6);
    if (action === 'capture') return '捕捉';
    return action;
  };

  // Pet attack animation — matches PHP splits(): skill! -damage !! + lifesteal
  const playPetAttack = (s: BattleState) => {
    const log = s.log;
    if (!log) return;
    clearAnimTimer();
    setPetImgSrc(s.petImgAck ? `/images/bb/${s.petImgAck}` : (s.petImg ? `/images/bb/${s.petImg}` : null));
    const sname = skillLabel(log.action);
    let parts: string[] = [];
    if (log.petCrit) parts.push('<span class="crit">暴击</span>');
    if (log.petMiss) {
      parts.push(sname + '! <span class="miss">miss</span> !');
    } else {
      parts.push(sname + '! <span class="dmg">-' + log.petDamage + '</span> !!');
      if (log.petLifeSteal > 0) parts.push('<span class="lifesteal">吸血 ' + log.petLifeSteal + '</span>');
    }
    setDamageText({ text: parts.join(' '), pos: 'right' });
    setAnimPhase('petHit');

    if (log.monsterDead) {
      // Monster killed — restore pet to standing, fade out monster, show KO + result
      setPetImgSrc(s.petImg ? `/images/bb/${s.petImg}` : null);
      setMonsterImgSrc(s.monsterImgDie ? `/images/gpc/${s.monsterImgDie}` : null);
      animTimer.current = setTimeout(() => {
        setAnimPhase('monsterDying');
        setCountdown(-1); // KO
        setFinished(true);
        setDamageText(null);
      }, 1500);
    } else {
      animTimer.current = setTimeout(() => {
        setPetImgSrc(s.petImg ? `/images/bb/${s.petImg}` : null);
        setAnimPhase('idle');
        setDamageText(null);
        if (s.phase === 'monster_turn') {
          setPhase('monster');
          animTimer.current = setTimeout(() => doMonsterTurn(s.sessionId!), 200);
        }
      }, 3000);
    }
  };

  // Monster attack animation — matches PHP gwF(): 怪物攻击! -damage !!
  const playMonsterAttack = (s: BattleState) => {
    const log = s.log;
    if (!log) return;
    clearAnimTimer();
    setMonsterImgSrc(s.monsterImgAck ? `/images/gpc/${s.monsterImgAck}` : (s.monsterImg ? `/images/gpc/${s.monsterImg}` : null));
    let parts: string[] = [];
    if (log.monsterMiss) {
      parts.push(s.monsterName + '攻击! <span class="miss">miss</span> !');
    } else {
      parts.push(s.monsterName + '攻击! <span class="dmg">-' + log.monsterDamage + '</span> !!');
    }
    setDamageText({ text: parts.join(' '), pos: 'left' });
    setAnimPhase('monsterHit');

    if (log.petDead) {
      // Pet killed — restore monster to standing, fade out pet, show KO + defeat
      setMonsterImgSrc(s.monsterImg ? `/images/gpc/${s.monsterImg}` : null);
      setPetImgSrc(s.petImgDie ? `/images/bb/${s.petImgDie}` : null);
      animTimer.current = setTimeout(() => {
        setAnimPhase('petDying');
        setCountdown(-1); // KO
        setFinished(true);
        setDamageText(null);
      }, 1500);
    } else {
      animTimer.current = setTimeout(() => {
        setMonsterImgSrc(s.monsterImg ? `/images/gpc/${s.monsterImg}` : null);
        setAnimPhase('idle');
        setDamageText(null);
        setPhase('waiting');
      }, 2000);
    }
  };

  const doAction = (action: string, skillId?: number) => {
    if (acting || finished || phase !== 'waiting') return;
    setActing(true);
    setPhase('pet');
    const body: Record<string, unknown> = { action, sessionId: state?.sessionId };
    if (skillId) body.skillId = skillId;
    apiPost<BattleState>('/battle/action', body).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) {
        const s = res.data;
        handleBattleResponse(s);
        playPetAttack(s);
      } else if (res.message) {
        setMsg(res.message); setTimeout(() => setMsg(null), 3000);
        setPhase('waiting');
      }
      setActing(false);
    }).catch((err) => { setActing(false); setPhase('waiting'); setMsg('操作失败: ' + (err?.response?.data?.message || err?.message || '网络错误')); setTimeout(() => setMsg(null), 3000); });
  };

  const doMonsterTurn = (sessionId: string) => {
    apiPost<BattleState>('/battle/monster-turn', { sessionId }).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) {
        const s = res.data;
        handleBattleResponse(s);
        playMonsterAttack(s);
      } else if (res.message) {
        setMsg(res.message); setTimeout(() => setMsg(null), 3000);
        setPhase('waiting');
      }
    }).catch((err) => { setPhase('waiting'); setMsg('怪物行动失败: ' + (err?.response?.data?.message || err?.message || '网络错误')); setTimeout(() => setMsg(null), 3000); });
  };

  const doFlee = () => {
    if (acting || finished || phase !== 'waiting') return;
    setActing(true);
    apiPost<BattleState>('/battle/flee', { sessionId: state?.sessionId }).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) handleBattleResponse(res.data);
      setActing(false);
    }).catch((err) => { setActing(false); setMsg('逃跑失败: ' + (err?.response?.data?.message || err?.message || '网络错误')); setTimeout(() => setMsg(null), 3000); });
  };

  const doCapture = (bagId: number) => {
    if (acting || finished || phase !== 'waiting') return;
    setActing(true);
    setPhase('pet');
    apiPost<BattleState>('/battle/action', { action: 'capture', bagId, sessionId: state?.sessionId }).then((res: ApiResponse<BattleState>) => {
      if (res.code === 0 && res.data) {
        const s = res.data;
        handleBattleResponse(s);
        if (s.captureSuccess) {
          setFinished(true); triggerRefresh();
        } else if (s.state !== 'WON' && s.state !== 'LOST' && s.phase === 'monster_turn') {
          playPetAttack(s);
        } else if (s.state !== 'WON' && s.state !== 'LOST') {
          setPhase('waiting');
        }
      } else if (res.message) {
        setMsg(res.message); setTimeout(() => setMsg(null), 3000);
        setPhase('waiting');
      }
      setActing(false);
    }).catch((err) => { setActing(false); setPhase('waiting'); setMsg('捕捉失败: ' + (err?.response?.data?.message || err?.message || '网络错误')); setTimeout(() => setMsg(null), 3000); });
  };

  if (loading) return <div className={styles.loading}>战斗中...</div>;

  // Cooldown page — matches PHP standalone cooldown page (replaces entire battle area)
  if (showCooldown) {
    return (
      <div className={styles.battleBg} style={{ backgroundImage: bgUrl }}>
        <div className={styles.cooldownPage}>
          <img src="/images/ui/fight/loading.gif" alt="" className={styles.cooldownSpinner} />
          <span className={styles.cooldownNum}>{cooldownSec}</span>
        </div>
      </div>
    );
  }

  if (error || !state) return <div className={styles.error}>{error || '初始化失败'}</div>;

  const ELEMENTS = ['','金','木','水','火','土'];
  const isOver = state.state === 'WON' || state.state === 'LOST';
  const petHpPct = Math.max(0, Math.min(100, (state.petHp / Math.max(1, state.petMaxHp)) * 100));
  const monsterHpPct = Math.max(0, Math.min(100, (state.monsterHp / Math.max(1, state.monsterMaxHp)) * 100));
  const petMpPct = Math.max(0, Math.min(100, (state.petMp / Math.max(1, state.petMaxMp)) * 100));
  const petExpPct = Math.max(0, Math.min(100, ((state.petNowexp ?? 0) / Math.max(1, state.petLexp ?? 100)) * 100));

  const petBodyCls = `${styles.petBody} ${animPhase === 'petHit' ? styles.petAttacking : ''} ${animPhase === 'petDying' ? styles.petDying : ''}`;
  const monsterBodyCls = `${styles.monsterBody} ${animPhase === 'monsterHit' ? styles.monsterAttacking : ''} ${animPhase === 'monsterDying' ? styles.monsterDying : ''}`;

  return (
    <div className={styles.battleBg} style={{ backgroundImage: bgUrl }}>
      {/* Center message */}
      {msg && <div className={styles.battleMsg} dangerouslySetInnerHTML={{ __html: msg }} />}

      {/* Damage text */}
      {damageText && (
        <div
          className={`${styles.damageText} ${damageText.pos === 'right' ? styles.damageRight : styles.damageLeft}`}
          dangerouslySetInnerHTML={{ __html: damageText.text }}
        />
      )}

      {/* Pet info — left side */}
      <div className={styles.petInfo}>
        <div className={styles.nameBar}>
          <span>{state.petName} Lv.{state.petLevel}</span>
        </div>
        <div className={styles.avatarRow}>
          <div className={styles.avatar}>
            {state.petHeadImg && <img src={`/images/bb/${state.petHeadImg}`} alt="" />}
          </div>
          <div className={styles.barsCol}>
            <div className={styles.barWrap}>
              <div className={styles.barFillHp} style={{ width: `${petHpPct}%` }} />
              <span className={styles.barText}>HP {state.petHp}/{state.petMaxHp}</span>
            </div>
            <div className={styles.barWrap}>
              <div className={styles.barFillMp} style={{ width: `${petMpPct}%` }} />
              <span className={styles.barText}>MP {state.petMp}/{state.petMaxMp}</span>
            </div>
            <div className={styles.barWrap}>
              <div className={styles.barFillExp} style={{ width: `${petExpPct}%` }} />
              <span className={styles.barText}>EXP {state.petNowexp ?? 0}/{state.petLexp ?? 100}</span>
            </div>
          </div>
        </div>
        <div className={petBodyCls}>
          {petImgSrc && <img src={petImgSrc} alt="" />}
        </div>
      </div>

      {/* Monster side — PHP absolute positioned elements */}
      <div className={styles.monsterEleBadge} />
      <div className={styles.monsterEleText}>{ELEMENTS[state.monsterWx ?? 0] ?? '?'}</div>
      <div className={styles.monsterHeader} />
      <div className={styles.monsterName}>{state.monsterName} Lv.{state.monsterLevel}</div>
      <div className={styles.monsterHpBar}>
        <div className={styles.monsterHpFill} style={{ width: `${monsterHpPct}%` }} />
      </div>
      <div className={styles.monsterHpText}>HP {state.monsterHp}/{state.monsterMaxHp}</div>
      <div className={monsterBodyCls}>
        {monsterImgSrc && <img src={monsterImgSrc} alt="" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />}
      </div>

      {/* Result — matches PHP #result overlay */}
      {isOver && (
        <div className={styles.result}>
          {state.state === 'WON' ? (
            <div className={styles.won}>
              <h3>战斗胜利！</h3>
              {state.expGained != null && <p>获得经验：{state.expGained}</p>}
              {state.moneyGained != null && <p>获得金币：{state.moneyGained}</p>}
              {state.levelUp && <p className={styles.levelUpText}>宠物升级到 Lv.{state.newLevel}！</p>}
              {state.drops && state.drops.length > 0 && (
                <p>掉落：{state.drops.map(d => d.name + ' x' + d.count).join('，')}</p>
              )}
              {state.captureSuccess && <p className={styles.captured}>成功捕捉!</p>}
            </div>
          ) : (
            <div className={styles.lost}>
              <h3>宝宝 {state.petName} 受到了严重伤害，已经不能战斗！！！</h3>
            </div>
          )}
          <div className={styles.resultBtns}>
            <span className={styles.continueBtn} onClick={handleContinue}>继续探险</span>
            <span className={styles.returnBtn} onClick={onClose}>返回村庄</span>
          </div>
        </div>
      )}

      {/* Action overlay when processing */}
      {acting && !damageText && <div className={styles.actingOverlay}>行动中...</div>}

      {/* Timer: countdown normally, KO on battle end */}
      {!isOver && <div className={styles.timer}>{countdown}</div>}
      {isOver && <div className={styles.timerKO}>KO</div>}

      {/* Bottom toolbar — PHP zdzsk.gif + 8 click areas */}
      {!isOver && (
        <div className={styles.toolbar}>
          <div className={`${styles.tb1} ${autoFight ? styles.tbActive : ''}`} onClick={() => setSubPanel(subPanel === 'autofight' ? null : 'autofight')} title="自动" />
          <div className={styles.tb2} onClick={() => setSubPanel(subPanel === 'autoskill' ? null : 'autoskill')} title="设置自动技能" />
          <div className={styles.tb3} onClick={() => doAction('attack')} title="攻击" />
          <div className={styles.tb4} onClick={() => setSubPanel(subPanel === 'skills' ? null : 'skills')} title="技能" />
          <div className={styles.tb5} onClick={openItemsPanel} title="辅助" />
          <div className={styles.tb6} onClick={openCapturePanel} title="捕捉" />
          <div className={styles.tb7} onClick={() => { setMsg('道具：暂不支持'); setTimeout(() => setMsg(null), 2000); }} title="道具" />
          <div className={styles.tb8} onClick={doFlee} title="逃跑" />
        </div>
      )}

      {/* Skill selection sub-panel */}
      {subPanel === 'skills' && state.skills && (
        <div className={styles.subPanel}>
          <div className={styles.subTitle}>选择技能</div>
          {/* 普通攻击 always available (PHP Usejn(1)) */}
          <div className={styles.skillItem}
            onClick={() => { setSubPanel(null); doAction('attack'); }}>
            <span className={styles.skillName}>普通攻击</span>
          </div>
          {state.skills.map(sk => (
            <div key={sk.id} className={styles.skillItem}
              onClick={() => { setSubPanel(null); doAction('skill', sk.id); }}>
              <span className={styles.skillName}>{sk.name} Lv.{sk.level}</span>
              <span className={styles.skillCost}>
                {sk.uhp > 0 && `HP${sk.uhp} `}{sk.ump > 0 && `MP${sk.ump}`}
              </span>
            </div>
          ))}
          <button className={styles.subCancel} onClick={() => setSubPanel(null)}>取消</button>
        </div>
      )}

      {/* Capture items panel — PHP loadtool(6) with catcharr */}
      {subPanel === 'capture' && (
        <div className={styles.subPanel}>
          <div className={styles.subTitle}>捕捉 {state.monsterName}</div>
          {catchItems.length === 0 ? (
            <p className={styles.subDesc}>没有可用的捕捉道具</p>
          ) : (
            catchItems.map(item => (
              <div key={item.id} className={styles.skillItem}
                onClick={() => { setSubPanel(null); doCapture(item.id); }}>
                <span className={styles.skillName}>{item.name}</span>
                <span className={styles.skillCost}>×{item.sums}</span>
              </div>
            ))
          )}
          <button className={styles.subCancel} onClick={() => setSubPanel(null)}>取消</button>
        </div>
      )}

      {/* Auto-fight panel — PHP loadtool(1): 4 options */}
      {subPanel === 'autofight' && (
        <div className={styles.subPanel}>
          <div className={styles.subTitle}>自动战斗</div>
          <div className={styles.autoGrid}>
            <div className={`${styles.autoOpt} ${autoFight && autoMode === 'gold' ? styles.autoOptActive : ''}`}
              onClick={() => handleAutoFight('gold')}>
              开始金币版<br/>自动攻击<br/><small>(1.2倍经验)</small>
            </div>
            <div className={`${styles.autoOpt}`}
              onClick={() => handleAutoFight('stop')}>
              关闭<br/>自动战斗
            </div>
            <div className={`${styles.autoOpt} ${autoFight && autoMode === 'yb' ? styles.autoOptActive : ''}`}
              onClick={() => handleAutoFight('yb')}>
              开始元宝版<br/>自动攻击<br/><small>(1.5倍经验)</small>
            </div>
            <div className={`${styles.autoOpt}`}
              onClick={() => handleAutoFight('stop')}>
              关闭<br/>自动战斗
            </div>
          </div>
          <button className={styles.subCancel} onClick={() => setSubPanel(null)}>取消</button>
        </div>
      )}

      {/* Auto-fight skill setting sub-panel */}
      {subPanel === 'autoskill' && (
        <div className={styles.subPanel}>
          <div className={styles.subTitle}>选择自动技能</div>
          <div className={`${styles.skillItem} ${!autoSkillId ? styles.skillActive : ''}`}
            onClick={() => { setAutoSkillId(null); setSubPanel(null); }}>
            <span className={styles.skillName}>普通攻击</span>
          </div>
          {state.skills?.map(sk => (
            <div key={sk.id} className={`${styles.skillItem} ${autoSkillId === sk.id ? styles.skillActive : ''}`}
              onClick={() => { setAutoSkillId(sk.id); setSubPanel(null); }}>
              <span className={styles.skillName}>{sk.name} Lv.{sk.level}</span>
              <span className={styles.skillCost}>
                {sk.uhp > 0 && `HP${sk.uhp} `}{sk.ump > 0 && `MP${sk.ump}`}
              </span>
            </div>
          ))}
          <button className={styles.subCancel} onClick={() => setSubPanel(null)}>取消</button>
        </div>
      )}

      {/* Items sub-panel — PHP useYao: show healing items from bag */}
      {subPanel === 'items' && (
        <div className={styles.subPanel}>
          <div className={styles.subTitle}>使用道具</div>
          {bagItems.length === 0 ? (
            <p className={styles.subDesc}>没有可用的恢复道具</p>
          ) : (
            bagItems.map(item => (
              <div key={item.id} className={styles.skillItem}
                onClick={() => doUseItem(item.id)}>
                <span className={styles.skillName}>{item.name}</span>
                <span className={styles.skillCost}>×{item.sums}</span>
              </div>
            ))
          )}
          <button className={styles.subCancel} onClick={() => setSubPanel(null)}>关闭</button>
        </div>
      )}
    </div>
  );
}
