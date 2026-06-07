import { useEffect, useState, useMemo } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
import type { ApiResponse, PropsItem } from '@/types';
import ItemTooltip from './ItemTooltip';
import styles from './BagPanel.module.css';

/** Lightweight bag item from API (UserBag fields only) */
interface BagItemBase {
  id: number; propId: number; count: number; vary: string;
  equipPetId: number | null; zbing: number; sell: number;
  cantrade?: number; holeInfo?: string; plusTimesEffect?: string; stime?: number;
}

/** Merged bag item with props data for display */
interface BagItemMerged extends BagItemBase {
  name?: string; img?: string; propsColor?: number; category?: string;
  requires?: string; effect?: string; buy?: number; yb?: number;
  usages?: string; propslock?: number; expire?: string;
  series?: string; serieseffect?: string; pluseffect?: string; prestige?: number;
  plusflag?: number; pluspid?: number; plusget?: string; plusnum?: number;
  postion?: number; varyname?: number;
  effectDesc?: string; requiresDesc?: string; usagesDesc?: string;
  serieseffectDesc?: string; pluseffectDesc?: string; holeInfoDesc?: string;
}

function mergeWithProps(base: BagItemBase, propsMap: Record<number, PropsItem>): BagItemMerged {
  const p = propsMap[base.propId];
  if (!p) return { ...base, name: `道具#${base.propId}` };
  const m: BagItemMerged = { ...base };
  m.name = p.name;
  m.img = p.img;
  m.varyname = p.varyname;
  m.propsColor = p.propscolor ? Number(p.propscolor) : undefined;
  m.effect = p.effect;
  m.effectDesc = p.effectDesc;
  m.requires = p.requires;
  m.requiresDesc = p.requiresDesc;
  m.buy = p.buy;
  m.yb = p.yb;
  m.usages = p.usages;
  m.usagesDesc = p.usagesDesc;
  m.propslock = p.propslock;
  m.series = p.series;
  m.serieseffect = p.serieseffect;
  m.serieseffectDesc = p.serieseffectDesc;
  m.pluseffect = p.pluseffect;
  m.pluseffectDesc = p.pluseffectDesc;
  m.prestige = p.prestige;
  m.plusflag = p.plusflag;
  m.pluspid = p.pluspid;
  m.plusget = p.plusget;
  m.plusnum = p.plusnum;
  m.postion = p.postion;
  m.category = p.category;
  // Expire: pass raw endtime so TooltipContent's getExpireStr can compute display
  if (p.endtime && p.endtime > 0) {
    m.expire = String(p.endtime);
  } else {
    m.expire = '0';
  }
  // Resolve holeInfoDesc
  if (base.holeInfo) {
    m.holeInfoDesc = base.holeInfo; // simplified; full resolution stays on backend for equip actions
  }
  return m;
}

interface PetBrief { id: number; name: string; level: number; }

const CATEGORIES: { label: string; vary: number[] }[] = [
  { label: '全部道具', vary: [] },
  { label: '辅助道具', vary: [1] },
  { label: '增益道具', vary: [2] },
  { label: '捕捉道具', vary: [3] },
  { label: '收集道具', vary: [4] },
  { label: '技能书',   vary: [5] },
  { label: '卡片道具', vary: [6] },
  { label: '进化道具', vary: [7] },
  { label: '合体道具', vary: [8] },
  { label: '装备道具', vary: [9] },
  { label: '精练道具', vary: [10] },
  { label: '宝箱道具', vary: [11] },
  { label: '特殊道具', vary: [12] },
  { label: '功能道具', vary: [13] },
  { label: '宠物卵',   vary: [14] },
  { label: '合成道具', vary: [15] },
];

export default function BagPanel() {
  const [baseItems, setBaseItems] = useState<BagItemBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const player = useAuthStore((s) => s.player);
  const maxBag = player?.maxBag ?? 30;
  const [tooltip, setTooltip] = useState<{ item: BagItemMerged; x: number; y: number } | null>(null);
  const propsMap = useGameStore((s) => s.propsMap);
  const setBag = useGameStore((s) => s.setBag);
  const setGamePets = useGameStore((s) => s.setPets);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const fetchPlayerAuth = useAuthStore((s) => s.fetchPlayer);
  const closePanel = useGameStore((s) => s.setActivePanel);

  // Merge base items with cached props
  const items: BagItemMerged[] = useMemo(
    () => baseItems.map(b => mergeWithProps(b, propsMap)),
    [baseItems, propsMap]
  );

  const fetchItems = () => {
    apiGet<BagItemBase[]>('/bag').then((res: ApiResponse<BagItemBase[]>) => {
      if (res.code === 0 && res.data) {
        setBaseItems(res.data);
        setBag(res.data.map((item) => {
          const p = propsMap[item.propId];
          return {
            id: item.id, name: p?.name ?? `道具#${item.propId}`,
            count: item.count, type: item.vary === 'equipment' ? 2 : 1, description: '',
          };
        }));
      }
    });
  };

  useEffect(() => {
    apiGet<BagItemBase[]>('/bag').then((bagRes) => {
      if (bagRes.code === 0 && bagRes.data) {
        setBaseItems(bagRes.data);
        setBag(bagRes.data.map((item) => {
          const p = propsMap[item.propId];
          return {
            id: item.id, name: p?.name ?? `道具#${item.propId}`,
            count: item.count, type: item.vary === 'equipment' ? 2 : 1, description: '',
          };
        }));
      }
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const filtered = items.filter(i => {
    if (i.count <= 0 || i.zbing === 1) return false;
    if (category === 0) return true;
    const varyList = CATEGORIES[category]?.vary ?? [];
    if (varyList.length === 0) return true;
    return varyList.includes(i.varyname ?? 0);
  });

  const usedCells = items.filter(i => i.count > 0 && i.zbing !== 1).length;

  const getCatLabel = (item: BagItemMerged) => {
    const c = CATEGORIES.find(c => c.vary.length > 0 && c.vary.includes(item.varyname ?? 0));
    return c?.label ?? item.category ?? '道具';
  };
  const selectedItem = items.find(i => i.id === selectedId);

  const PET_STAT_KEYS = ['hp:','mp:','addexp','addczl','addac','addmc','addhp','addmp','addspeed','addhits','addmiss'];
  const needsPet = (item: BagItemMerged) => {
    if (item.varyname === 9) return false;
    const eff = item.effect || '';
    return PET_STAT_KEYS.some(k => eff.includes(k));
  };

  const mainPetId = useAuthStore((s) => s.player?.mbid);
  const selectedPetId = useGameStore((s) => s.selectedPetId);
  const activePetId = selectedPetId ?? mainPetId;

  const handleUse = (item: BagItemMerged) => {
    if (needsPet(item)) {
      const targetPetId = item.varyname === 9 ? (mainPetId ?? activePetId) : activePetId;
      if (!targetPetId) { alert('请先在牧场设置主战宠物！'); return; }
      doUse(item, targetPetId);
    } else {
      doUse(item, 0);
    }
  };

  const doUse = (item: BagItemMerged, petId: number) => {
    setBaseItems(prev => prev.map(i => i.id === item.id ? { ...i, count: i.count - 1 } : i));
    apiPost<Record<string, unknown>>(`/bag/use/${item.id}`, { petId }).then((res: ApiResponse<Record<string, unknown>>) => {
      if (res.code === 0 && res.data) {
        const d = res.data;
        const err = d.error as string;
        const msg = d.message as string;
        if (err) { systips(err); fetchItems(); return; }
        if (d.equipped) systips(`装备成功！${d.propName} 穿戴到 ${d.slotName}${d.replaced ? '(替换旧装备)' : ''}`);
        else if (d.unequipped) systips(`已卸下装备`);
        else if (d.type === 'healHP') systips(`${item.name} 为宠物恢复了 ${d.healedHP} 点HP`);
        else if (d.type === 'healMP') systips(`${item.name} 为宠物恢复了 ${d.healedMP} 点MP`);
        else if (d.type === 'exp' && d.levelUp) systips(`${item.name} 使宠物升级到 Lv.${d.newLevel}！`);
        else if (d.type === 'bagExpand' || d.type === 'depotExpand') {
          systips(msg ?? `扩容成功`);
          if (d.type === 'bagExpand' && d.newMaxBag) fetchPlayerAuth(true);
        } else if (d.type === 'yuanbao') systips(msg ?? `获得${d.ybGained}元宝`);
        else if (d.type === 'crystal') systips(msg ?? `获得水晶`);
        else if (d.type === 'openMap') systips(msg ?? '地图已解锁');
        else if (d.type === 'openPet') systips(msg ?? `恭喜获得宠物：${d.petName}！`);
        else if (d.type === 'chest') {
          const items = d.items as { propId: number; count: number; name?: string }[] | undefined;
          if (items && items.length > 0) {
            const names = items.map(i => `${i.name ?? '道具#' + i.propId}x${i.count}`).join('、');
            systips(`${msg} 获得：${names}`);
          } else {
            systips(msg ?? '使用成功');
          }
        }
        else if (msg) systips(msg);
        else systips(`使用了 ${item.name}`);
        fetchItems();
        apiGet<PetBrief[]>('/pets').then((r) => {
          if (r.code === 0 && r.data) {
            setGamePets(r.data.map((p: PetBrief) => ({ ...p, hp: 0, mp: 0, atk: 0, def: 0, speed: 0, element: '金' as const, quality: 0, exp: 0 })));
          }
        });
        if (item.count <= 1) setSelectedId(null);
      } else {
        systips(res.message ?? '使用失败');
        fetchItems();
      }
    }).catch((err: any) => {
      const msg = err?.response?.data?.message;
      systips(msg || '使用失败');
      fetchItems();
    });
  };

  const handleSell = (item: BagItemMerged) => {
    apiPost<Record<string, unknown>>('/bag/sell/' + item.id, { count: 1 }).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data;
        systips(`出售 ${d.sold} x${d.count}，获得 ${d.goldGained} 金币`);
      } else { systips(res.message ?? '出售失败'); }
      fetchItems(); triggerRefresh();
    }).catch((err: any) => { systips(err?.response?.data?.message || '出售失败'); });
  };

  const handleDrop = (item: BagItemMerged) => {
    if (!confirm(`确定丢弃 ${item.name} 吗？`)) return;
    apiPost<Record<string, unknown>>('/bag/drop/' + item.id, {}).then((res) => {
      if (res.code === 0) systips(`已丢弃 ${item.name}`);
      else systips(res.message ?? '丢弃失败');
      fetchItems(); setSelectedId(null);
    }).catch((err: any) => { systips(err?.response?.data?.message || '丢弃失败'); });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      <button className={styles.closeBtn} onClick={() => closePanel(null)} />
      <span className={styles.space}>当前背包空间：{usedCells}/{maxBag}</span>
      <select className={styles.filter} value={category} onChange={(e) => setCategory(Number(e.target.value))}>
        {CATEGORIES.map((c, i) => (<option key={i} value={i}>{c.label}</option>))}
      </select>
      <div className={styles.colHeader}>
        <span className={styles.colIcon}>图标</span>
        <span className={styles.colName}>物品名称</span>
        <span className={styles.colType}>类型</span>
        <span className={styles.colCount}>数量</span>
      </div>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={4} className={styles.empty}>背包空空如也，去商城看看吧！</td></tr>
            ) : (
              filtered.map((item) => (
                <tr
                  key={item.id}
                  className={`${styles.row} ${selectedId === item.id ? styles.rowSelected : ''}`}
                  onClick={() => setSelectedId(selectedId === item.id ? null : item.id)}
                  onDoubleClick={() => handleUse(item)}
                  onMouseEnter={(e) => setTooltip({ item, x: e.clientX, y: e.clientY })}
                  onMouseMove={(e) => setTooltip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null)}
                  onMouseLeave={() => setTooltip(null)}
                >
                  <td className={styles.tdIcon}>
                    {item.varyname && <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" className={styles.iconImg}
                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />}
                  </td>
                  <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                  <td className={styles.tdType}>{getCatLabel(item)}</td>
                  <td className={styles.tdCount}>x{item.count}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <div className={styles.actions}>
        <button className={styles.btn} disabled={!selectedItem || selectedItem?.varyname === 3}
          onClick={() => selectedItem && handleUse(selectedItem)}>使用</button>
        <button className={styles.btn} disabled={!selectedItem}
          onClick={() => selectedItem && handleSell(selectedItem)}>出售</button>
        <button className={styles.btn} disabled={!selectedItem}
          onClick={() => selectedItem && handleDrop(selectedItem)}>丢弃</button>
        <button className={styles.btnDepot} disabled={!selectedItem}
          onClick={() => selectedItem && alert('放入仓库功能开发中')}>放入仓库</button>
      </div>
      {tooltip && <ItemTooltip item={tooltip.item} x={tooltip.x} y={tooltip.y} />}
    </div>
  );
}
