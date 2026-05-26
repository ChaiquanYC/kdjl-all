import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types';
import DungeonPanel from './DungeonPanel';
import TowerPanel from './TowerPanel';
import ChallengePanel from './ChallengePanel';
import styles from './MapPanel.module.css';

interface MapInfo {
  id: number; name: string; desc: string; level: string; unlocked: boolean; img?: string; gpclist?: string; needs?: string;
}

interface PetBrief { id: number; name: string; level: number; cardImg?: string; }

interface MonsterInfo {
  id: number; name: string; level: number; hp: number; boss: number; img?: string;
}

interface MapPoint {
  id: number; name: string; left: number; top: number; width: number; height: number;
  page: number; open?: boolean; dungeon?: boolean; pvp?: boolean; tower?: boolean; teamDungeon?: boolean; challenge?: boolean;
}

// PHP map locations — page 1 (map6.jpg), page 2 (map3.jpg), page 3 (map5.jpg)
const MAP_POINTS: MapPoint[] = [
  { id: 1,  name: '新手训练营', left: 329, top: 255, width: 83, height: 24, page: 1 },
  { id: 2,  name: '妖精森林',   left: 236, top: 192, width: 83, height: 24, page: 1 },
  { id: 3,  name: '潮汐海涯',   left: 334, top: 155, width: 83, height: 24, page: 1 },
  { id: 16, name: '海底世界',   left: 475, top: 135, width: 83, height: 24, page: 1 },
  { id: 4,  name: '巨石山脉',   left: 37,  top: 38,  width: 83, height: 24, page: 1 },
  { id: 5,  name: '黄金陵',     left: 644, top: 282, width: 83, height: 24, page: 1 },
  { id: 6,  name: '炽热沙滩',   left: 28,  top: 148, width: 83, height: 24, page: 1 },
  { id: 7,  name: '尤玛火山',   left: 234, top: 72,  width: 83, height: 24, page: 1 },
  { id: 8,  name: '死亡沙漠',   left: 166, top: 136, width: 83, height: 24, page: 1 },
  { id: 9,  name: '海市盛楼',   left: 126, top: 271, width: 83, height: 24, page: 1 },
  { id: 10, name: '冰滩',       left: 688, top: 82,  width: 83, height: 24, page: 1 },
  // 副本入口 — PHP tpl_map.html exact positions
  { id: 11, name: '伊苏王神墓', left: 695, top: 226, width: 90, height: 24, page: 1, dungeon: true },
  { id: 12, name: '火龙王宫殿', left: 153, top: 32,  width: 90, height: 24, page: 1, dungeon: true },
  { id: 13, name: '史芬克斯穴', left: 22,  top: 232, width: 90, height: 24, page: 1, dungeon: true },
  { id: 14, name: '玲珑城',     left: 523, top: 38,  width: 90, height: 24, page: 1, dungeon: true },
  { id: 151,name: '辉煌大道',   left: 523, top: 226, width: 90, height: 24, page: 1, dungeon: true },
  // 特殊地点
  { id: 15, name: '圣诞小屋',   left: 367, top: 95,  width: 90, height: 24, page: 1, open: false },
  // 组队副本 (multi_monsters=3) — 遗忘宫殿, requires team
  { id: 128,name: '遗忘宫殿',   left: 535, top: 177, width: 90, height: 24, page: 1, teamDungeon: true },
  // 战场入口 — 未开放
  { id: 152,name: '神圣战场',   left: 507, top: 285, width: 77, height: 28, page: 1, open: false },
  // Page 2 (map3.jpg) — PHP tpl_mapnew.html
  { id: 100, name: '石阵',      left: 130, top: 124, width: 75, height: 24, page: 2 },
  { id: 103, name: '平原',      left: 208, top: 157, width: 75, height: 24, page: 2 },
  { id: 112, name: '鬼屋',      left: 98,  top: 211, width: 75, height: 24, page: 2 },
  { id: 106, name: '绿荫林',    left: 45,  top: 59,  width: 75, height: 24, page: 2 },
  { id: 115, name: '天空之城',  left: 419, top: 82,  width: 75, height: 24, page: 2 },
  { id: 118, name: '天之路',    left: 465, top: 17,  width: 75, height: 24, page: 2 },
  { id: 121, name: '危之路',    left: 509, top: 184, width: 75, height: 24, page: 2 },
  { id: 109, name: '五指石印',  left: 263, top: 226, width: 75, height: 24, page: 2 },
  // 副本入口 — Page 2
  { id: 50,  name: '厄非斯深渊', left: 170, top: 280, width: 75, height: 24, page: 2, dungeon: true },
  { id: 124, name: '阿尔提密林', left: 441, top: 249, width: 75, height: 24, page: 2, dungeon: true },
  { id: 127, name: '菲拉苛地域', left: 239, top: 99,  width: 88, height: 24, page: 2, dungeon: true },
  // Page 3 (map5.jpg) — PHP tpl_mapnew1.html
  { id: 140, name: '巨石荒野',   left: 206, top: 53,  width: 75, height: 24, page: 3 },
  { id: 137, name: '迷雾森林',   left: 552, top: 26,  width: 72, height: 24, page: 3 },
  { id: 134, name: '孢子林',     left: 573, top: 277, width: 75, height: 24, page: 3 },
  { id: 145, name: '蓝泪之泉',   left: 664, top: 107, width: 75, height: 24, page: 3 },
  { id: 148, name: '赎罪之塔',   left: 70,  top: 270, width: 75, height: 24, page: 3 },
  { id: 131, name: '埋骨之地',   left: 27,  top: 180, width: 73, height: 24, page: 3 },
  // 副本入口 — Page 3
  { id: 143, name: '熔岩地宫',   left: 54,  top: 26,  width: 89, height: 24, page: 3, dungeon: true },
  { id: 144, name: '幻魔之境',   left: 301, top: 269, width: 76, height: 24, page: 3, dungeon: true },
  // 通天塔
  { id: 126, name: '通天塔',     left: 380, top: 160, width: 75, height: 24, page: 3, tower: true },
  // 挑战模式
  { id: 125, name: '琥珀屋',     left: 500, top: 200, width: 75, height: 24, page: 3, challenge: true },
];

interface TeamMemberInfo { playerId: number; nickname: string; state: number; }
interface TeamInfo { id: number; name: string; creatorId: number; members: TeamMemberInfo[]; }
interface TeamListItem { id: number; name: string; creatorId: number; memberCount: number; }

interface Props {
  onChallenge: (monsterId: number, monsterName: string, mapId?: number, mapImg?: string) => void;
}

export default function MapPanel({ onChallenge }: Props) {
  const [maps, setMaps] = useState<MapInfo[]>([]);
  const [mapPage, setMapPage] = useState(1);
  const [selectedMap, setSelectedMap] = useState<MapInfo | null>(null);
  const [monsters, setMonsters] = useState<MonsterInfo[]>([]);
  const [loadingMonsters, setLoadingMonsters] = useState(false);
  const [pets, setLocalPets] = useState<PetBrief[]>([]);
  const [onlinePlayers, setOnlinePlayers] = useState<{id:number;nickname:string;level:number}[]>([]);
  const [selectedPetId, setSelectedPetId] = useState<number | null>(null);
  const [dungeonMapId, setDungeonMapId] = useState<number | null>(null);
  const [towerMapId, setTowerMapId] = useState<number | null>(null);
  const [challengeMapId, setChallengeMapId] = useState<number | null>(null);
  const [myTeam, setMyTeam] = useState<TeamInfo | null>(null);
  const [teamList, setTeamList] = useState<TeamListItem[]>([]);
  const [showTeamJoin, setShowTeamJoin] = useState(false);
  const setCurrentMap = useGameStore((s) => s.setCurrentMap);
  const setPets = useGameStore((s) => s.setPets);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const refreshTrigger = useGameStore((s) => s.refreshTrigger);
  const setBattleDifficulty = useGameStore((s) => s.setBattleDifficulty);
  const [difficulty, setDifficulty] = useState(1);
  const leaveMap = () => { apiPost('/player/leave-map', {}).then(() => { setSelectedMap(null); setMonsters([]); triggerRefresh(); }); };

  const fetchMyTeam = () => {
    apiGet<TeamInfo>('/team/my').then((res) => {
      if (res.code === 0 && res.data) setMyTeam(res.data);
      else setMyTeam(null);
    }).catch(() => setMyTeam(null));
  };

  useEffect(() => {
    Promise.all([
      apiGet<MapInfo[]>('/map/list'),
      apiGet<PetBrief[]>('/pets'),
    ]).then(([mapRes, petRes]) => {
      if (mapRes.code === 0 && mapRes.data) setMaps(mapRes.data);
      if (petRes.code === 0 && petRes.data) setLocalPets(petRes.data);
    }).catch(() => {});
    fetchMyTeam();
  }, [refreshTrigger]);

  const handleCreateTeam = () => {
    const name = prompt('请输入队伍名称：');
    if (!name) return;
    apiPost<{created:boolean;teamId:number}>('/team/create', { name }).then((res) => {
      if (res.code === 0) { fetchMyTeam(); alert('队伍创建成功！'); }
      else alert(res.message || '创建失败');
    }).catch(() => alert('创建失败'));
  };

  const handleJoinTeam = (teamId: number) => {
    apiPost<{joined:boolean}>('/team/join/' + teamId, {}).then((res) => {
      if (res.code === 0) { fetchMyTeam(); setShowTeamJoin(false); alert('加入队伍成功！'); }
      else alert(res.message || '加入失败');
    }).catch(() => alert('加入失败'));
  };

  const player = useAuthStore((s) => s.player);
  const isTeamLeader = myTeam && player ? myTeam.creatorId === player.id : false;

  const handleLeaveTeam = () => {
    if (!confirm('确定要离开队伍吗？')) return;
    apiPost<{left:boolean}>('/team/leave', {}).then((res) => {
      if (res.code === 0) { setMyTeam(null); alert('已离开队伍'); }
      else alert(res.message || '离开失败');
    }).catch(() => alert('离开失败'));
  };

  const handleKickMember = (memberId: number, nickname: string) => {
    if (!confirm(`确定要踢出 ${nickname} 吗？`)) return;
    apiPost<{kicked:boolean}>('/team/kick/' + memberId, {}).then((res) => {
      if (res.code === 0) { fetchMyTeam(); }
      else alert(res.message || JSON.stringify(res.data));
    }).catch(() => alert('踢人失败'));
  };

  const handleApproveMember = (memberId: number) => {
    apiPost<{approved:boolean}>('/team/approve/' + memberId, {}).then((res) => {
      if (res.code === 0) { fetchMyTeam(); }
      else alert(res.message || '审批失败');
    }).catch(() => alert('审批失败'));
  };

  const handleToggleAway = () => {
    apiPost<{away:boolean}>('/team/away', {}).then((res) => {
      if (res.code === 0) { fetchMyTeam(); }
      else alert(res.message || JSON.stringify(res.data));
    }).catch(() => alert('操作失败'));
  };

  const setGameView = useGameStore((s) => s.setGameView);

  const handleEnterMap = (mapId: number) => {
    const point = MAP_POINTS.find(p => p.id === mapId);
    if (point && point.open === false) { alert(point.name + '\n暂未开放，敬请期待！'); return; }
    if (point && point.pvp) { setGameView('pvp'); return; }
    // Dungeons use dedicated DungeonPanel (PHP tpl_fb.html)
    if (point && point.dungeon) { setDungeonMapId(mapId); return; }
    if (point && point.tower) { setTowerMapId(mapId); return; }
    if (point && point.challenge) { setChallengeMapId(mapId); return; }
    if (point && point.teamDungeon) {
      if (!myTeam) { alert('遗忘宫殿为组队副本，需要先创建或加入队伍！'); return; }
      if (!isTeamLeader) { alert('只有队长才能进入组队副本！'); return; }
      // Enter as regular map but with team requirement met
      const map = maps.find(m => m.id === mapId);
      if (!map) { alert('地图数据未加载'); return; }
      setSelectedMap(map);
      setCurrentMap(map.id);
      apiPost('/player/enter-map/' + map.id, {}).then(() => triggerRefresh()).catch(() => {});
      setLoadingMonsters(true);
      fetchMyTeam();
      Promise.all([
        apiGet<MonsterInfo[]>(`/map/${map.id}/monsters`),
        apiGet<{id:number;nickname:string;level:number}[]>(`/map/${map.id}/players`),
      ]).then(([mRes, pRes]) => {
        if (mRes.code === 0 && mRes.data) setMonsters(mRes.data);
        if (pRes.code === 0 && pRes.data) setOnlinePlayers(pRes.data);
        setLoadingMonsters(false);
      }).catch(() => setLoadingMonsters(false));
      return;
    }
    const map = maps.find(m => m.id === mapId);
    if (!map) { alert('地图数据未加载'); return; }
    if (!map.unlocked) {
      const needsStr = map.needs || '';
      let unlockMsg = '该地图尚未解锁！';
      if (needsStr) {
        const parts = needsStr.split(':');
        if (parts[0] === 'needww') {
          const detail = (parts[1] || '').split('|');
          unlockMsg = `需要 ${detail[0] || '?'} 威望来解锁此地图`;
        } else if (parts[0] === 'needitem' || parts[0] === 'needtime') {
          const detail = (parts[1] || '').split('|');
          unlockMsg = `需要消耗道具来解锁此地图（道具ID: ${detail[0] || '?'}）`;
        }
      }
      if (!confirm(unlockMsg + '\n\n是否尝试解锁？')) return;
      apiPost<{unlocked:boolean}>('/map/unlock/' + mapId, {}).then((res) => {
        if (res.code === 0 && res.data?.unlocked) {
          alert('地图解锁成功！');
          // Refresh map list to update unlock state
          apiGet<MapInfo[]>('/map/list').then((r) => {
            if (r.code === 0 && r.data) {
              setMaps(r.data);
              const refreshed = r.data.find(m => m.id === mapId);
              if (refreshed?.unlocked) handleEnterMap(mapId);
            }
          });
        } else alert(res.message || '解锁失败');
      }).catch(() => alert('解锁请求失败'));
      return;
    }
    setSelectedMap(map);
    setCurrentMap(map.id);
    apiPost('/player/enter-map/' + map.id, {}).then(() => triggerRefresh()).catch(() => {});
    setLoadingMonsters(true);
    fetchMyTeam();
    setShowTeamJoin(false);
    Promise.all([
      apiGet<MonsterInfo[]>(`/map/${map.id}/monsters`),
      apiGet<{id:number;nickname:string;level:number}[]>(`/map/${map.id}/players`),
    ]).then(([mRes, pRes]) => {
      if (mRes.code === 0 && mRes.data) setMonsters(mRes.data);
      if (pRes.code === 0 && pRes.data) setOnlinePlayers(pRes.data);
      setLoadingMonsters(false);
    }).catch(() => setLoadingMonsters(false));
  };

  const handleCapture = (monsterId: number) => {
    apiPost<Record<string, unknown>>('/pets/capture/' + monsterId, {}).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string, unknown>;
        if (d.captured) {
          alert('捕捉成功！获得了 ' + String(d.petName ?? '') + ' Lv.' + String(d.petLevel ?? '1'));
          apiGet<unknown[]>('/pets').then((r) => {
            if (r.code === 0 && r.data) {
              setPets(r.data.map((p: any) => ({
                id: p.id, name: p.name, level: p.level,
                hp: 0, mp: 0, atk: 0, def: 0, speed: 0,
                element: '金' as const, quality: 0, exp: 0,
              })));
            }
          });
        } else {
          alert('捕捉失败！(概率 ' + String(d.roll ?? '?') + '/' + String(d.threshold ?? '?') + ')');
        }
      } else {
        alert(res.message ?? '捕捉失败');
      }
    });
  };

  // Dungeon panel — PHP tpl_fb.html
  if (dungeonMapId) {
    return (
      <DungeonPanel
        mapId={dungeonMapId}
        onChallenge={onChallenge}
        onLeave={() => setDungeonMapId(null)}
      />
    );
  }

  // Tower panel (通天塔)
  if (towerMapId) {
    return (
      <TowerPanel
        onChallenge={onChallenge}
        onLeave={() => setTowerMapId(null)}
      />
    );
  }

  // Challenge panel (挑战模式)
  if (challengeMapId) {
    return (
      <ChallengePanel
        onChallenge={onChallenge}
        onLeave={() => setChallengeMapId(null)}
      />
    );
  }

  // World map view — matches PHP Expore_Mod
  if (!selectedMap) {
    return (
      <div className={styles.worldMap}>
        <img
          src={mapPage === 1 ? '/images/ui/map/map6.jpg' : mapPage === 2 ? '/images/ui/map/map3.jpg' : '/images/ui/map/map5.jpg'}
          alt="世界地图"
          className={styles.worldBg}
        />

        {MAP_POINTS.filter(p => p.page === mapPage).map((p) => {
          const isClosed = p.open === false;
          const isDungeon = p.dungeon === true;
          const isPvp = p.pvp === true;
          const isTower = p.tower === true;
          const isTeamDun = p.teamDungeon === true;
          // PHP: special map types are small invisible click areas, no label image
          if (isDungeon || isPvp || isTower || isTeamDun || p.challenge) {
            const label = isDungeon ? '副本：' : isPvp ? '战场：' : isTower ? '通天塔：' : isTeamDun ? '组队副本：' : '挑战：';
            return (
              <div
                key={p.id}
                className={styles.mapPoint}
                style={{ left: p.left, top: p.top, width: p.width || 20, height: p.height || 20 }}
                onClick={() => handleEnterMap(p.id)}
                title={label + p.name}
              />
            );
          }
          const map = maps.find(m => m.id === p.id);
          const unlocked = isClosed ? false : (map?.unlocked ?? false);
          const imgId = unlocked ? p.id : '03';
          return (
            <div
              key={p.id}
              className={`${styles.mapPoint} ${isClosed ? styles.mapPointClosed : (unlocked ? styles.unlocked : styles.locked)}`}
              style={{ left: p.left, top: p.top, width: p.width, height: p.height }}
              onClick={() => handleEnterMap(p.id)}
              title={isClosed ? p.name + '(未开放)' : p.name}
            >
              <img src={`/images/ui/map1/${imgId}.png`} alt={p.name} className={styles.mapLabel} />
            </div>
          );
        })}

        {/* Navigation arrows — PHP style 3-page navigation */}
        {mapPage === 1 && (
          <>
            <div className={`${styles.worldArrow} ${styles.arrowUp}`} onClick={() => setMapPage(2)} title="切换地图">
              <img src="/images/ui/map/up.gif" alt="切换地图" />
            </div>
            <div className={`${styles.worldArrow} ${styles.arrowRight}`} onClick={() => setMapPage(3)} title="切换地图">
              <img src="/images/ui/map/arrr.gif" alt="切换地图" />
            </div>
          </>
        )}
        {(mapPage === 2 || mapPage === 3) && (
          <div className={`${styles.worldArrow} ${styles.arrowLeft}`} onClick={() => setMapPage(1)}>
            <img src="/images/ui/map/left.gif" alt="上一页" />
          </div>
        )}
      </div>
    );
  }

  // Monster area — 3-column layout with PHP backgrounds
  return (
    <div className={styles.teamContainer}>
      <div className={styles.teamLeft}>
        <div className={styles.teamDesc}>
          <strong>探险 — {selectedMap?.name ?? ''}</strong>
          <p>{selectedMap?.desc ?? ''}</p>
          <p>怪物等级：{selectedMap?.level ?? ''} 级</p>
          {selectedMap?.gpclist && <p className={styles.monsterNames}>出现怪物：{selectedMap.gpclist}</p>}
        </div>
        <div className={styles.teamPet}>
          <div className={styles.teamCards}>
            {pets.slice(0,3).map((p) => (
              <div key={p.id} className={`${styles.teamCard} ${selectedPetId === p.id ? styles.petSelected : ''}`}
                onClick={() => setSelectedPetId(p.id)} title={`${p.name} Lv.${p.level}`}>
                {p.cardImg ? <img src={`/images/bb/${p.cardImg}`} alt="" /> : <span>{p.name}</span>}
                <span className={styles.petLevel}>Lv.{p.level}</span>
              </div>
            ))}
            {pets.length < 3 && Array.from({length: 3-pets.length}).map((_, i) => (
              <div key={'e'+i} className={styles.teamCard} />
            ))}
          </div>
        </div>
      </div>

      <div className={styles.teamCenter}>
        {/* Monster list — PHP .zdzd_gpc */}
        <div className={styles.monsterList}>
          {monsters.length === 0 ? (
            <span className={styles.teamListEmpty}>此地图暂无怪物</span>
          ) : (
            monsters.map(m => (
              <div key={m.id} className={styles.monsterRow}>
                {m.img && <img src={`/images/gpc/${m.img}`} alt="" className={styles.monsterIcon} />}
                <span className={styles.monsterName}>{m.name}</span>
                <span className={styles.monsterLv}>Lv.{m.level}</span>
                <span className={styles.monsterHp}>HP:{m.hp}</span>
                <button className={styles.challengeBtn}
                  onClick={() => {
                    const selPet = selectedPetId ? pets.find(p => p.id === selectedPetId) : pets[0];
                    if (!selPet) { alert('请先选择一只宠物！'); return; }
                    const mapMinLevel = parseInt(selectedMap?.level?.split(',')[0] || '1');
                    if (selPet.level < mapMinLevel) { alert('宠物等级不足，无法挑战此地图！'); return; }
                    onChallenge(m.id, m.name, selectedMap?.id, selectedMap?.img);
                  }}>挑战</button>
                <button className={styles.captureBtn} onClick={() => handleCapture(m.id)}>捕捉</button>
              </div>
            ))
          )}
        </div>
        {/* PHP: Team panel — full for maps >=100 or ==16, simple otherwise */}
        {!(selectedMap && (selectedMap.id >= 100 || selectedMap.id === 16)) ? (
          <div className={styles.teamList}><span className={styles.teamListEmpty}>暂未组队</span></div>
        ) : (myTeam ? (
          <div className={styles.teamList}>
            <div className={styles.teamNameRow}>
              <span className={styles.teamName}>{myTeam.name}</span>
              <div style={{display:'flex',gap:4}}>
                {!isTeamLeader && (
                  <button className={styles.teamActionBtn} onClick={handleToggleAway} style={{fontSize:10,padding:'1px 6px'}}>暂离</button>
                )}
                <button className={styles.leaveBtn} onClick={handleLeaveTeam}>离开</button>
              </div>
            </div>
            {myTeam.members.map(m => (
              <div key={m.playerId} className={styles.teamMemberRow}>
                <img src="/images/ui/team/ren.gif" alt="" className={styles.renIcon} />
                <span className={styles.teamMemberName}>{m.nickname}</span>
                <span className={styles.teamMemberState}>
                  {m.state === -1 ? '⏳待审批' : m.state === 0 ? '💤暂离' : '✅'}
                </span>
                {isTeamLeader && m.playerId !== player?.id && (
                  <div style={{display:'flex',gap:2}}>
                    {m.state === -1 && (
                      <button className={styles.teamActionBtn} style={{fontSize:10,padding:'0 4px'}}
                        onClick={() => handleApproveMember(m.playerId)}>审批</button>
                    )}
                    <button className={styles.teamActionBtn} style={{fontSize:10,padding:'0 4px',background:'#c04040'}}
                      onClick={() => handleKickMember(m.playerId, m.nickname)}>踢出</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : showTeamJoin ? (
          <div className={styles.teamList}>
            <div className={styles.teamNameRow}>
              <span className={styles.teamName}>选择队伍</span>
              <button className={styles.leaveBtn} onClick={() => setShowTeamJoin(false)}>返回</button>
            </div>
            {teamList.length === 0 ? (
              <span className={styles.teamListEmpty}>暂无队伍</span>
            ) : (
              teamList.map(t => (
                <div key={t.id} className={styles.teamMemberRow} style={{cursor:'pointer'}} onClick={() => handleJoinTeam(t.id)}>
                  <span className={styles.teamMemberName}>{t.name}</span>
                  <span style={{fontSize:10,color:'#888'}}>{t.memberCount}/4人</span>
                </div>
              ))
            )}
          </div>
        ) : (
          <div className={styles.teamList}>
            <span className={styles.teamListEmpty}>暂未组队</span>
            <div className={styles.teamActions}>
              <button className={styles.teamActionBtn} onClick={handleCreateTeam}>创建队伍</button>
              <button className={styles.teamActionBtn} onClick={() => {
                apiGet<TeamListItem[]>('/team/list').then((res) => {
                  if (res.code === 0 && res.data) { setTeamList(res.data); setShowTeamJoin(true); }
                }).catch(() => alert('获取队伍列表失败'));
              }}>加入队伍</button>
            </div>
          </div>)
        )}
        {/* PHP Tt_Mod.php:34 — difficulty only for map id >= 100 or == 16 */}
        {selectedMap && (selectedMap.id >= 100 || selectedMap.id === 16) && (
        <div className={styles.diffBar}>
          <span className={styles.diffLabel}>难度：{['普通','困难','冒险'][difficulty-1]}</span>
          <div className={styles.diffBtns}>
            <img src="/images/ui/team/ann07.gif" alt="普通" className={`${styles.diffBtn} ${difficulty===1?styles.diffActive:''}`}
              onClick={() => setDifficulty(1)} />
            <img src="/images/ui/team/ann08.gif" alt="困难" className={`${styles.diffBtn} ${difficulty===2?styles.diffActive:''}`}
              onClick={() => setDifficulty(2)} />
            <img src="/images/ui/team/ann09.gif" alt="冒险" className={`${styles.diffBtn} ${difficulty===3?styles.diffActive:''}`}
              onClick={() => setDifficulty(3)} />
          </div>
        </div>
        )}
        <div className={styles.teamActions}>
          <img src="/images/ui/team/zd.gif" alt="战斗" className={styles.anniuBtn}
            onClick={() => {
              if (monsters.length === 0) { alert('此地图暂无怪物'); return; }
              setBattleDifficulty(difficulty);
              const m = monsters[Math.floor(Math.random() * monsters.length)];
              // Team leader: team fight (only for maps >=100 or ==16)
              if (isTeamLeader && myTeam && selectedMap && (selectedMap.id >= 100 || selectedMap.id === 16)) {
                const activeCount = myTeam.members.filter(mb => mb.state === 1).length;
                if (activeCount < 1) { alert('没有活跃的队员！'); return; }
                if (!confirm(`发起团队战斗？\n活跃队员：${activeCount}人\n难度：${['普通','困难','冒险'][difficulty-1]}\n怪物：${m.name}`)) return;
                apiPost<Record<string,unknown>>('/battle/team-fight', { monsterId: m.id, difficulty }).then((res) => {
                  if (res.code === 0 && res.data) {
                    const d = res.data as Record<string,unknown>;
                    const won = d.won;
                    const rewards = d.rewards as Array<Record<string,unknown>> || [];
                    let msg = won ? '团队战斗胜利！\n\n队员奖励：' : '团队战斗失败！\n';
                    rewards.forEach(r => {
                      msg += `\n${r.petName}：EXP +${r.expGained || 0}${r.leveledUp ? ' ⬆升级' : ''} 金币 +${r.moneyGained || 0}`;
                    });
                    alert(msg);
                    triggerRefresh();
                  } else alert(res.message || '团队战斗失败');
                }).catch(() => alert('团队战斗请求失败'));
                return;
              }
              // Solo fight: need to select pet
              const selPet = selectedPetId ? pets.find(p => p.id === selectedPetId) : pets[0];
              if (!selPet) { alert('请先选择一只宠物！'); return; }
              const mapMinLevel = parseInt(selectedMap?.level?.split(',')[0] || '1');
              if (selPet.level < mapMinLevel) { alert('宠物等级不足，无法挑战此地图！'); return; }
              onChallenge(m.id, m.name, selectedMap?.id, selectedMap?.img);
            }} />
          <img src="/images/ui/team/lk.gif" alt="离开" className={styles.anniuBtn}
            onClick={() => { leaveMap(); setSelectedMap(null); }} />
        </div>
      </div>

      <div className={styles.teamRight}>
        <div className={styles.teamOnline}><span>在线玩家 ({onlinePlayers.length})</span></div>
        <div className={styles.playerList}>
          {onlinePlayers.length === 0 ? (
            <div className={styles.noPlayers}>暂无其他玩家</div>
          ) : (
            onlinePlayers.map(p => (
              <div key={p.id} className={styles.playerItem}>
                <img src="/images/ui/team/ren.gif" alt="" className={styles.renIcon} />
                <span>{p.nickname}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
