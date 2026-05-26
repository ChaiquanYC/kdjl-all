import { useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useGameStore, type Panel } from '@/stores/gameStore';
import { useChat } from '@/hooks/useWebSocket';
import { apiGet, apiPost } from '@/api/client';
import ChatPanel from '@/components/game/ChatPanel';
import PetList from '@/components/game/PetList';
import BagPanel from '@/components/game/BagPanel';
import MapPanel from '@/components/game/MapPanel';
import CityPanel from '@/components/game/CityPanel';
import BattlePanel from '@/components/game/BattlePanel';
import EquipPanel from '@/components/game/EquipPanel';
import ShopPanel from '@/components/game/ShopPanel';
import ZbPanel from '@/components/game/ZbPanel';
import SmShopPanel from '@/components/game/SmShopPanel';
import TaskPanel from '@/components/game/TaskPanel';
import GuildPanel from '@/components/game/GuildPanel';
import RankPanel from '@/components/game/RankPanel';
import GmPanel from '@/components/game/GmPanel';
import TeamPanel from '@/components/game/TeamPanel';
import AuctionPanel from '@/components/game/AuctionPanel';
import InheritPanel from '@/components/game/InheritPanel';
import MarryPanel from '@/components/game/MarryPanel';
import DepotPanel from '@/components/game/DepotPanel';
import RanchPanel from '@/components/game/RanchPanel';
import FriendPanel from '@/components/game/FriendPanel';
import PvpPanel from '@/components/game/PvpPanel';
import ZhanBuPanel from '@/components/game/ZhanBuPanel';
import SdPanel from '@/components/game/SdPanel';
import PlayerInfoPanel from '@/components/game/PlayerInfoPanel';
import OverlayPanel from './OverlayPanel';
import styles from './GameLayout.module.css';


interface PetBrief { id: number; name: string; level: number }

export default function GameLayout() {
  const player = useAuthStore((s) => s.player);
  const fetchPlayer = useAuthStore((s) => s.fetchPlayer);
  const addChatMessage = useGameStore((s) => s.addChatMessage);
  const activePanel = useGameStore((s) => s.activePanel);
  const setActivePanel = useGameStore((s) => s.setActivePanel);
  const gameView = useGameStore((s) => s.gameView);
  const setGameView = useGameStore((s) => s.setGameView);
  const inBattle = useGameStore((s) => s.inBattle);
  const battlePet = useGameStore((s) => s.battlePet);
  const battleMonster = useGameStore((s) => s.battleMonster);
  const battleMapId = useGameStore((s) => s.battleMapId);
  const startBattle = useGameStore((s) => s.startBattle);
  const endBattle = useGameStore((s) => s.endBattle);
  const refreshTrigger = useGameStore((s) => s.refreshTrigger);

  // Pet selection before battle
  const [pendingMonster, setPendingMonster] = useState<{ id: number; name: string } | null>(null);
  const [playerPets, setPlayerPets] = useState<PetBrief[]>([]);

  // Online reward status
  const [rewardStatus, setRewardStatus] = useState<{
    canClaim: boolean; currentStep: number; totalSteps: number;
    onlineMinutes: number; remainingSeconds: number; nextThresholdMinutes: number;
  } | null>(null);

  const checkReward = () => {
    apiGet<{
      canClaim: boolean; currentStep: number; totalSteps: number;
      onlineMinutes: number; remainingSeconds: number; nextThresholdMinutes: number;
    }>('/daily/online-reward/check').then((r) => {
      if (r.code === 0 && r.data) setRewardStatus(r.data);
    });
  };

  const formatRemaining = (seconds: number) => {
    if (seconds <= 0) return '可领取';
    const m = Math.ceil(seconds / 60);
    return m >= 60 ? `还需${Math.floor(m/60)}时${m%60}分` : `还需${m}分钟`;
  };

  const [challengeMapId, setChallengeMapId] = useState<number | null>(null);
  const [challengeMapImg, setChallengeMapImg] = useState<string | null>(null);

  const handleChallenge = (monsterId: number, monsterName: string, mapId?: number, mapImg?: string) => {
    setChallengeMapId(mapId ?? null);
    setChallengeMapImg(mapImg ?? null);
    apiGet<PetBrief[]>('/pets').then((res) => {
      if (res.code === 0 && res.data) {
        if (res.data.length === 0) {
          alert('没有可出战的宠物！');
          return;
        }
        if (res.data.length === 1) {
          startBattle(res.data[0].id, res.data[0].name, monsterId, monsterName, mapId);
        } else {
          // If player has a main pet (mbid), use it directly
          const mainPet = player?.mbid ? res.data.find(p => p.id === player.mbid) : null;
          if (mainPet) {
            startBattle(mainPet.id, mainPet.name, monsterId, monsterName, mapId);
          } else {
            setPlayerPets(res.data);
            setPendingMonster({ id: monsterId, name: monsterName });
          }
        }
      }
    });
  };

  const handleSelectPet = (pet: PetBrief) => {
    if (pendingMonster) {
      startBattle(pet.id, pet.name, pendingMonster.id, pendingMonster.name, challengeMapId ?? undefined);
      setPendingMonster(null);
    }
  };

  const DUNGEON_IDS = new Set([11, 12, 13, 14, 50, 124, 127, 143, 144, 151]);

  // Continue battle: fetch random monster from same map, start new battle
  const handleContinueBattle = () => {
    const mapId = battleMapId || challengeMapId;
    if (!mapId) { endBattle(); return; }
    // Dungeon: advance to next wave
    if (mapId && DUNGEON_IDS.has(mapId)) {
      apiPost<Record<string,unknown>>(`/dungeon/${mapId}/next-wave`, {}).then((res) => {
        if (res.code === 0 && res.data) {
          const d = res.data as Record<string,unknown>;
          if (d.completed) {
            alert('副本通关！所有波次已清除！');
            endBattle();
            setGameView('map');
          } else {
            const pet = battlePet!;
            startBattle(pet.id, pet.name, d.monsterId as number, d.monsterName as string, mapId ?? undefined);
          }
        } else if (res.message?.includes('未进入')) {
          // Not in dungeon anymore, fall back to regular fight
          endBattle();
          setGameView('map');
        } else {
          alert(res.message || '加载下一波失败');
          endBattle();
          setGameView('map');
        }
      }).catch(() => { endBattle(); });
      return;
    }
    apiGet<{ id: number; name: string }[]>(`/map/${mapId}/monsters`).then((res) => {
      if (res.code === 0 && res.data && res.data.length > 0) {
        const m = res.data[Math.floor(Math.random() * res.data.length)];
        const pet = battlePet!;
        startBattle(pet.id, pet.name, m.id, m.name, mapId ?? undefined);
      } else {
        endBattle();
      }
    });
  };

  // Return from battle: go back to map view
  const handleReturnFromBattle = () => {
    endBattle();
    setGameView('map');
  };

  // Server time
  const [serverTime, setServerTime] = useState('');
  useEffect(() => {
    const update = () => {
      const now = new Date();
      setServerTime(
        now.getHours().toString().padStart(2, '0') + ':' +
        now.getMinutes().toString().padStart(2, '0') + ':' +
        now.getSeconds().toString().padStart(2, '0')
      );
    };
    update();
    const timer = setInterval(update, 1000);
    return () => clearInterval(timer);
  }, []);

  // Online players count
  const [onlineCount, setOnlineCount] = useState(0);
  useEffect(() => {
    apiGet<{ count: number }>('/player/online/count').then((r) => {
      if (r.code === 0 && r.data) setOnlineCount(r.data.count);
    }).catch(() => {});
    const timer = setInterval(() => {
      apiGet<{ count: number }>('/player/online/count').then((r) => {
        if (r.code === 0 && r.data) setOnlineCount(r.data.count);
      }).catch(() => {});
    }, 60000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => { fetchPlayer(); }, [fetchPlayer, refreshTrigger]);
  useEffect(() => { checkReward(); }, [refreshTrigger]);

  useChat((msg) => {
    addChatMessage(msg);
  });

  return (
    <div className={styles.container}>
      {/* Main page: 1000px centered */}
      <div className={styles.page}>
        {/* Top section: left sidebar(197) + content(803) */}
        <div className={styles.main_t}>
          <div className={styles.side}>
            <div className={styles.sideBtns}>
              <a className={`${styles.sideBtn} ${gameView==='map'?styles.sideBtnActive:''}`} onClick={()=>{if(inBattle)endBattle();setGameView('map')}}>
                <img src="/images/ui/menu/m_fight.png" alt="野外探险" />
              </a>
              <a className={`${styles.sideBtn} ${gameView==='city'?styles.sideBtnActive:''}`} onClick={()=>{if(inBattle)endBattle();setGameView('city')}}>
                <img src="/images/ui/menu/m_city.png" alt="城镇" />
              </a>
              <a className={`${styles.sideBtn} ${gameView==='pets'?styles.sideBtnActive:''}`} onClick={()=>{if(inBattle)endBattle();setGameView('pets')}}>
                <img src="/images/ui/menu/m_pet.png" alt="宠物" />
              </a>
              <a className={`${styles.sideBtn} ${gameView==='profile'?styles.sideBtnActive:''}`} onClick={()=>{if(inBattle)endBattle();setGameView('profile')}}>
                <img src="/images/ui/menu/m_info.png" alt="个人信息" />
              </a>
            </div>
          </div>
          <div className={styles.content}>
            <div className={styles.toolsBar}>
              {([
                { label:'好友', panel:'friend' as Panel },
                { label:'背包', panel:'bag' as Panel },
                { label:'任务', panel:'tasks' as Panel },
              ]).map(btn => (
                <a key={btn.label}
                  className={btn.panel && activePanel===btn.panel ? styles.toolsActive : ''}
                  onClick={() => btn.panel ? setActivePanel(activePanel===btn.panel ? null : btn.panel) : null}>
                  {btn.label}
                </a>
              ))}
            </div>
            <div className={styles.gameBox}>
              {inBattle && battlePet && battleMonster ? (
                <BattlePanel key={`${battlePet.id}-${battleMonster.id}`} petId={battlePet.id} monsterId={battleMonster.id} mapId={battleMapId ?? undefined} mapImg={challengeMapImg ?? undefined} onClose={handleReturnFromBattle} onContinue={handleContinueBattle} />
              ) : pendingMonster ? (
                <div className={styles.petSelect}><h3>选择出战宠物</h3>
                  <div className={styles.petGrid}>{playerPets.map(p=><div key={p.id} className={styles.petOption} onClick={()=>handleSelectPet(p)}><span>{p.name}</span><span>Lv.{p.level}</span></div>)}</div>
                  <button className={styles.cancelBtn} onClick={()=>setPendingMonster(null)}>取消</button>
                </div>
              ) : gameView==='ranch' ? (
                <RanchPanel/>
              ) : gameView==='pvp' ? (
                <PvpPanel/>
              ) : gameView==='zhanbu' ? (
                <ZhanBuPanel/>
              ) : gameView==='sd' ? (
                <SdPanel/>
              ) : gameView==='auction' ? (
                <AuctionPanel/>
              ) : gameView==='smshop' ? (
                <SmShopPanel/>
              ) : gameView==='zb' ? (
                <ZbPanel/>
              ) : gameView==='shop' ? (
                <ShopPanel/>
              ) : gameView==='depot' ? (
                <DepotPanel/>
              ) : gameView==='city' ? (
                <CityPanel/>
              ) : gameView==='map' ? (
                <MapPanel onChallenge={handleChallenge}/>
              ) : gameView==='pets' ? (
                <PetList/>
              ) : gameView==='profile' ? (
                <PlayerInfoPanel/>
              ) : (
                <div className={styles.welcomeBox}>
                  <div className={styles.welcomeLeft}>
                    <dt>新宠亮相</dt>
                    <dd>欢迎回来，<b>{player?.nickname??'冒险者'}</b>！</dd>
                    <div className={styles.petShow}>
                      <img src="/images/welcome/petnew.jpg" alt="新宠" />
                    </div>
                  </div>
                  <div className={styles.welcomeRight}>
                    <h2 className={styles.annTitle}>最新公告</h2>
                    <ul>
                      <li>★ 注意安全防盗防骗</li>
                      <li>★ 去地图挑战怪物升级</li>
                      <li>★ 使用道具恢复宠物HP</li>
                      <li>★ 捕捉怪物需要精灵球</li>
                      <li>★ 公会组队更多奖励</li>
                    </ul>
                    <p className={styles.welcomeText}>
                      欢迎来到口袋精灵2！在这个奇幻的世界里，你可以捕捉、培养各种可爱的宠物精灵，
                      与它们一起冒险成长。去野外探险，挑战强大的怪物，让你的宠物变得更加强大吧！
                    </p>
                    <a className={styles.gocityBtn} onClick={() => setGameView('city')}>
                      <img src="/images/welcome/gocity.gif" alt="进入城镇" />
                    </a>
                  </div>
                </div>
              )}
            </div>

            {/* Overlay panels — float on top, not clipped by main_t overflow:visible */}
            {activePanel==='bag' && <OverlayPanel title="背包" width={370} height={410} defaultLeft={250} defaultTop={0} showHeader={false} onClose={()=>setActivePanel(null)}><BagPanel/></OverlayPanel>}
            {activePanel==='equip' && <OverlayPanel title="装备" width={420} height={380} defaultLeft={180} defaultTop={20} onClose={()=>setActivePanel(null)}><EquipPanel/></OverlayPanel>}
            {activePanel==='tasks' && <OverlayPanel title="任务" width={550} height={400} defaultLeft={130} defaultTop={15} onClose={()=>setActivePanel(null)}><TaskPanel/></OverlayPanel>}
            {activePanel==='guild' && <OverlayPanel title="公会" width={450} height={380} defaultLeft={180} defaultTop={20} onClose={()=>setActivePanel(null)}><GuildPanel/></OverlayPanel>}
            {activePanel==='rank' && <OverlayPanel title="排行" width={450} height={400} defaultLeft={180} defaultTop={15} onClose={()=>setActivePanel(null)}><RankPanel/></OverlayPanel>}
            {activePanel==='team' && <OverlayPanel title="组队" width={420} height={380} defaultLeft={200} defaultTop={20} onClose={()=>setActivePanel(null)}><TeamPanel/></OverlayPanel>}
{activePanel==='inherit' && <OverlayPanel title="传承" width={420} height={380} defaultLeft={200} defaultTop={20} onClose={()=>setActivePanel(null)}><InheritPanel/></OverlayPanel>}
            {activePanel==='marry' && <OverlayPanel title="婚姻" width={420} height={380} defaultLeft={200} defaultTop={20} onClose={()=>setActivePanel(null)}><MarryPanel/></OverlayPanel>}
            {activePanel==='friend' && <OverlayPanel title="好友" width={420} height={380} defaultLeft={200} defaultTop={20} onClose={()=>setActivePanel(null)}><FriendPanel/></OverlayPanel>}
            {activePanel==='gm' && <OverlayPanel title="GM工具" width={500} height={420} defaultLeft={150} defaultTop={10} onClose={()=>setActivePanel(null)}><GmPanel/></OverlayPanel>}
          </div>
        </div>

        {/* Bottom: chat area */}
        <div className={styles.main_b}>
          <div className={styles.chatL}>
            <div className={styles.chatStatus}>
              <span className={styles.onlineCount}>在线玩家：{onlineCount}</span>
              <span className={styles.serverTime}>{serverTime}</span>
              {player?.dblExpFlag && player.dblExpFlag > 1 && (
                <span className={styles.doubleExp} title="双倍经验生效中">
                  {player.dblExpFlag === 2 ? '1.5x' : player.dblExpFlag === 3 ? '2x' : player.dblExpFlag === 4 ? '2.5x' : '3x'}经验
                </span>
              )}
              <img
                src={rewardStatus?.canClaim ? '/images/getch.gif' : '/images/getop.jpg'}
                alt="在线奖励"
                className={rewardStatus?.canClaim ? styles.floatRewardImg : styles.floatRewardImgGray}
                onClick={() => {
                  if (!rewardStatus?.canClaim) { alert('还没有到领取时间！'); return; }
                  apiPost('/daily/online-reward', {}).then((r: any) => {
                    alert(r.data?.message || (r.code === 0 ? '领取成功!' : r.message));
                    fetchPlayer(); checkReward();
                  });
                }}
                title={rewardStatus ? `在线奖励 (${rewardStatus.currentStep}/${rewardStatus.totalSteps}) ${rewardStatus.canClaim ? '可领取' : formatRemaining(rewardStatus.remainingSeconds)}` : '在线奖励'}
              />
            </div>
            <ChatPanel embedded />
            <div className={styles.chatInputW}>
              <input id="chatmsg" placeholder="输入聊天内容..." onKeyDown={(e)=>{if(e.key==='Enter'){const inp=document.getElementById('chatmsg') as HTMLInputElement;if(inp?.value.trim()){useGameStore.getState().addChatMessage({id:Date.now().toString(),senderId:player?.id||0,senderName:player?.nickname||'?',content:inp.value,channel:'world',timestamp:Date.now()});inp.value='';}}}} />
              <button onClick={()=>{const inp=document.getElementById('chatmsg') as HTMLInputElement;if(inp?.value.trim()){useGameStore.getState().addChatMessage({id:Date.now().toString(),senderId:player?.id||0,senderName:player?.nickname||'?',content:inp.value,channel:'world',timestamp:Date.now()});inp.value='';}}}>发送</button>
            </div>
          </div>
          <div className={styles.tipR}></div>
        </div>
      </div>

      {/* Footer — decorative background only, matching PHP */}
      <div className={styles.footer}></div>
    </div>
  );
}
