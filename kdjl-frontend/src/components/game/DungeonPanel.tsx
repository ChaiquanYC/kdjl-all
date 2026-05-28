import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './DungeonPanel.module.css';

interface PetBrief { id: number; name: string; level: number; cardImg?: string; }
interface DungeonData {
  id: number; name: string; desc: string; level: number; cooldown: number;
  waveCount: number; progress: number; onCooldown: boolean; cooldownRemaining: number;
  monsters: Array<{id:number;name:string;level:number;hp:number}>; crystalCost?: number;
}

interface Props {
  mapId: number;
  onChallenge: (monsterId: number, monsterName: string, mapId?: number, mapImg?: string) => void;
  onLeave: () => void;
}

const DUNGEON_IMG: Record<number, string> = {
  11: '/images/fuben/fbdt02.jpg', 12: '/images/fuben/fbdt10.jpg',
  13: '/images/fuben/fbdt11.jpg', 14: '/images/fuben/fbdt14.jpg',
  50: '/images/fuben/fbdt50.jpg', 124: '/images/fuben/fbdt124.jpg',
  127: '/images/fuben/fbdt127.jpg', 143: '/images/fuben/fbdt143.jpg',
  144: '/images/fuben/fbdt144.jpg', 151: '/images/fuben/fbdt02.jpg',
};

export default function DungeonPanel({ mapId, onChallenge, onLeave }: Props) {
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const player = useAuthStore((s) => s.player);
  const [dungeon, setDungeon] = useState<DungeonData | null>(null);
  const [pets, setLocalPets] = useState<PetBrief[]>([]);
  const selectedPetId = useGameStore((s) => s.selectedPetId);
  const setSelectedPetId = useGameStore((s) => s.setSelectedPetId);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      apiGet<DungeonData>(`/dungeon/${mapId}`),
      apiGet<PetBrief[]>('/pets'),
    ]).then(([dRes, pRes]) => {
      if (dRes.code === 0 && dRes.data) setDungeon(dRes.data as DungeonData);
      if (pRes.code === 0 && pRes.data) {
        setLocalPets(pRes.data);
        if (!selectedPetId && pRes.data.length > 0) {
          const mainPet = player?.mbid ? pRes.data.find(p => p.id === player.mbid) : null;
          setSelectedPetId(mainPet ? mainPet.id : pRes.data[0].id);
        }
      }
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [mapId]);

  const handleEnter = () => {
    if (!selectedPetId && pets.length === 0) { alert('请先选择一只宠物！'); return; }
    const petId = selectedPetId || pets[0]?.id;
    const pet = pets.find(p => p.id === petId);
    if (!pet) { alert('请先选择一只宠物！'); return; }
    if (dungeon && pet.level < dungeon.level) { alert('宠物等级不足！需要Lv.' + dungeon.level); return; }
    apiPost<Record<string,unknown>>(`/dungeon/${mapId}/enter`, {}).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string,unknown>;
        triggerRefresh();
        onChallenge(d.monsterId as number, d.monsterName as string, mapId, undefined);
      } else alert(res.message || '进入副本失败');
    }).catch(() => alert('进入副本失败'));
  };

  const handleSkipCooldown = () => {
    if (!confirm('确定用水晶跳过冷却吗？')) return;
    apiPost<Record<string,unknown>>(`/dungeon/${mapId}/skip-cooldown`, {}).then((res) => {
      if (res.code === 0) { alert('冷却已重置！'); setDungeon(prev => prev ? {...prev, onCooldown: false, cooldownRemaining: 0} : null); }
      else alert(res.message || '跳过失败');
    }).catch(() => alert('跳过失败'));
  };

  if (loading) return <div className={styles.container}><div className={styles.loading}>加载中...</div></div>;

  const img = DUNGEON_IMG[mapId] || '/images/fuben/fbdt02.jpg';
  const nextMonster = dungeon?.monsters?.[dungeon.progress];
  const cooldownStr = dungeon?.onCooldown
    ? `${Math.floor((dungeon.cooldownRemaining || 0) / 3600)}时${Math.floor(((dungeon.cooldownRemaining || 0) % 3600) / 60)}分`
    : '已开启';

  return (
    <div className={styles.container}>
      {/* Main table: left(686) + sep(2) + right(241) + sep(2) — PHP tpl_fb.html line 24-103 */}
      <div className={styles.mainTable}>
        {/* LEFT COLUMN: 686px */}
        <div className={styles.leftCol}>
          {/* Top banner — PHP line 28: #img# + fbdt03(262x58) + fbdt04(101x58) */}
          <div className={styles.banner}>
            <img src={img} alt="" className={styles.dungeonImg} />
            <img src="/images/fuben/fbdt03.jpg" width="262" height="58" alt="" />
            <img src="/images/fuben/fbdt04.jpg" width="101" height="58" alt="" />
          </div>

          {/* Description — PHP line 31 */}
          <div className={styles.descArea}>
            <p className={styles.descText}>{dungeon?.desc ?? ''}</p>
          </div>

          {/* Bottom border — PHP line 40 */}
          <img src="/images/fuben/fbdt06.jpg" className={styles.bannerBottom} alt="" />

          {/* Lower area: pets (50%) + info (50%) — PHP line 43-76 */}
          <div className={styles.lowerArea}>
            <div className={styles.petSection}>
              <div className={styles.petHeader}><img src="/images/ui/team/fb01.jpg" alt="" /></div>
              <div className={styles.petCards}>
                {pets.slice(0, 3).map((p, i) => (
                  <img key={p.id}
                    src={`/images/bb/${p.cardImg || 'g1.gif'}`}
                    alt={p.name}
                    title={`${p.name} Lv.${p.level}`}
                    className={styles.petImg}
                    style={{opacity: selectedPetId ? (selectedPetId === p.id ? 1 : 0.5) : (i === 0 ? 1 : 0.5)}}
                    onClick={() => setSelectedPetId(p.id)}
                  />
                ))}
              </div>
            </div>
            <div className={styles.infoSection}>
              <div className={styles.infoHeader}><img src="/images/ui/team/fb02.jpg" alt="" /></div>
              <table className={styles.infoTable}>
                <tbody>
                  <tr><td>副本等级：{dungeon?.level ?? '?'}级</td></tr>
                  <tr><td>副本倒计时：{cooldownStr}</td></tr>
                  <tr><td>怪物总数：{dungeon?.waveCount ?? '?'}</td></tr>
                  <tr><td>当前进度：{dungeon ? dungeon.progress + 1 : 1}</td></tr>
                  <tr><td>即将面对：{nextMonster?.name || '?'}</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* SEPARATOR — PHP line 78-85 */}
        <div className={styles.separator} />

        {/* RIGHT COLUMN: 241px — PHP line 86-101 */}
        <div className={styles.rightCol}>
          <img src="/images/ui/team/fb03.jpg" className={styles.rightHeader} alt="" />
          <div className={styles.rightContent}>
            <span className={styles.teamPlaceholder}>暂未组队</span>
          </div>
          <div className={styles.rightActions}>
            {dungeon?.onCooldown ? (
              <button className={styles.cooldownBtn} onClick={handleSkipCooldown}>
                {cooldownStr}<br/>水晶跳过
              </button>
            ) : (
              <img src="/images/ui/team/fb04.jpg" className={styles.actionBtn} onClick={handleEnter} alt="进入" />
            )}
            <img src="/images/ui/team/fb05.jpg" className={styles.actionBtn} onClick={onLeave} alt="离开" />
          </div>
        </div>

        {/* RIGHT SEPARATOR — PHP line 102 */}
        <div className={styles.separator} />
      </div>
    </div>
  );
}
