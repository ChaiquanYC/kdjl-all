import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { formatEffectText, parseEffects } from '@/utils/equipEffect';
import styles from './EquipPanel.module.css';

interface EquipItem { id: number; propId: number; equipPetId: number | null; zbing: number;
  holeInfo: string | null; name?: string; img?: string; effect?: string; }
interface PetBrief { id: number; name: string; level: number; img?: string; }

const SLOTS = [
  { pos: 0, name: '武器', icon: '⚔️' }, { pos: 1, name: '衣服', icon: '🛡️' },
  { pos: 2, name: '头盔', icon: '⛑️' }, { pos: 3, name: '鞋子', icon: '👢' },
  { pos: 4, name: '项链', icon: '📿' }, { pos: 5, name: '戒指左', icon: '💍' },
  { pos: 6, name: '戒指右', icon: '💍' }, { pos: 7, name: '护腕', icon: '🤲' },
  { pos: 8, name: '腰带', icon: '🎗️' }, { pos: 9, name: '特殊', icon: '✨' },
  { pos: 10, name: '翅膀', icon: '🪽' },
];

export default function EquipPanel() {
  const [equipment, setEquipment] = useState<EquipItem[]>([]);
  const [pets, setPets] = useState<PetBrief[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPet, setSelectedPet] = useState<PetBrief | null>(null);
  const [zbStr, setZbStr] = useState<string>('');
  const [msg, setMsg] = useState<string | null>(null);
  const setBag = useGameStore((s) => s.setBag);

  const fetchData = () => {
    Promise.all([
      apiGet<EquipItem[]>('/bag/equipment'),
      apiGet<PetBrief[]>('/pets'),
    ]).then(([eqRes, petRes]) => {
      if (eqRes.code === 0 && eqRes.data) setEquipment(eqRes.data);
      if (petRes.code === 0 && petRes.data) {
        setPets(petRes.data);
        if (petRes.data.length > 0) {
          setSelectedPet(petRes.data[0]);
          fetchZb(petRes.data[0].id);
        }
      }
      setLoading(false);
    });
  };

  useEffect(() => { fetchData(); }, []);

  const fetchZb = (petId: number) => {
    apiGet<Record<string,unknown>>('/pets/' + petId).then((res) => {
      if (res.code === 0 && res.data) setZbStr((res.data as any).zb || '');
    });
  };

  const handlePetSelect = (pet: PetBrief) => {
    setSelectedPet(pet);
    fetchZb(pet.id);
  };

  const handleEquip = (equipId: number) => {
    if (!selectedPet) return;
    apiPost('/bag/equip/' + equipId, { petId: selectedPet.id }).then((res: any) => {
      if (res.code === 0) { fetchData(); fetchZb(selectedPet.id); refreshBag(); }
      else setMsg(res.message);
      if (res.message) setTimeout(() => setMsg(null), 2500);
    }).catch((err: any) => {
      setMsg(err?.response?.data?.message || '装备失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const handleUnequip = (equipId: number) => {
    apiPost('/bag/unequip/' + equipId, {}).then((res: any) => {
      if (res.code === 0) { fetchData(); if (selectedPet) fetchZb(selectedPet.id); refreshBag(); }
      else setMsg(res.message);
      if (res.message) setTimeout(() => setMsg(null), 2500);
    }).catch((err: any) => {
      setMsg(err?.response?.data?.message || '卸下装备失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const refreshBag = () => {
    apiGet<unknown[]>('/bag').then((r: any) => {
      if (r.code === 0 && r.data) setBag(r.data.map((item: any) => ({
        id: item.id, name: item.name ?? '道具', count: item.count,
        type: item.vary === 'equipment' ? 2 : 1, description: '' })));
    });
  };

  // Parse zb string "pos:bagId,pos:bagId"
  const zbMap: Record<number, number> = {};
  if (zbStr) {
    zbStr.split(',').forEach((p) => {
      const [pos, id] = p.split(':');
      if (pos && id) zbMap[Number(pos)] = Number(id);
    });
  }

  // Find equipment item by userbag id
  const findEquip = (bagId: number) => equipment.find((e) => e.id === bagId);
  // Calculate total equipment bonuses from equipped items
  const totalBonuses: Record<string, number> = {};
  Object.values(zbMap).forEach((bagId) => {
    const item = findEquip(bagId);
    if (item?.effect) {
      item.effect.split(',').forEach((eff: string) => {
        const [k, v] = eff.split(':');
        if (k && v) totalBonuses[k] = (totalBonuses[k] || 0) + (Number(v) || 0);
      });
    }
  });

  // Find unused equipment (not in any slot)
  const unusedEquip = equipment.filter((e) => e.zbing === 0);

  if (loading) return <div className={styles.loading}>加载中...</div>;
  if (pets.length === 0) return <div className={styles.empty}>没有可穿戴装备的宠物</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      {/* Pet selector */}
      <div className={styles.petSelector}>
        <span className={styles.label}>选择宠物：</span>
        {pets.map((pet) => (
          <button key={pet.id} className={selectedPet?.id === pet.id ? styles.petBtnActive : styles.petBtn}
            onClick={() => handlePetSelect(pet)}>{pet.name} Lv.{pet.level}</button>
        ))}
      </div>

      {Object.keys(totalBonuses).length > 0 && (
        <div className={styles.bonusBar}>
          <span className={styles.bonusLabel}>装备加成:</span>
          {parseEffects(Object.entries(totalBonuses).map(([k, v]) => `${k}:${v}`).join(',')).map((e, i) => (
            <span key={i} className={styles.bonusItem}>{e.value > 0 ? '+' : ''}{e.value}{e.isPercent ? '%' : ''}{e.label}</span>
          ))}
        </div>
      )}

      <div className={styles.mainArea}>
        <div className={styles.petPreview}>
          {selectedPet?.img && <img src={`/images/bb/${selectedPet.img}`} className={styles.petPreviewImg} alt="" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />}
          <div className={styles.petPreviewName}>{selectedPet?.name} Lv.{selectedPet?.level}</div>
          <div className={styles.petPreviewStats}>装备: {Object.keys(zbMap).length}/10 槽位</div>
        </div>
        {/* 10-slot equipment grid */}
        <div className={styles.slotGrid}>
          {SLOTS.map((slot) => {
            const bagId = zbMap[slot.pos];
            const item = bagId ? findEquip(bagId) : null;
            return (
              <div key={slot.pos} className={`${styles.slot} ${item ? styles.slotFilled : styles.slotEmpty}`}
                onClick={() => item ? handleUnequip(item.id) : null} title={item ? `点击卸下: ${item.name}` : `${slot.name} - 空`}>
                <div className={styles.slotIcon}>{slot.icon}</div>
                <div className={styles.slotName}>{slot.name}</div>
                {item ? (
                  <div className={styles.slotItem}>
                    {item.img && <img src={`/images/props/${item.img}`} className={styles.slotItemImg} alt="" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />}
                    <span className={styles.slotItemName}>{item.name}</span>
                    {item.effect && <span className={styles.slotEffect}>{formatEffectText(item.effect)}</span>}
                  </div>
                ) : (
                  <div className={styles.slotEmptyText}>空</div>
                )}
              </div>
            );
          })}
        </div>

        {/* Available equipment list */}
        <div className={styles.equipList}>
          <h3>可穿戴装备 ({unusedEquip.length})</h3>
          {unusedEquip.length === 0 ? (
            <div className={styles.noEquip}>没有可穿戴的装备，去商城看看吧</div>
          ) : (
            <div className={styles.equipGrid}>
              {unusedEquip.map((item) => (
                <div key={item.id} className={styles.equipCard} onClick={() => handleEquip(item.id)}>
                  <span className={styles.equipName}>{item.name ?? `装备#${item.propId}`}</span>
                  {item.effect && <span className={styles.equipEffect}>{formatEffectText(item.effect)}</span>}
                  <button className={styles.wearBtn}>穿戴</button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
