import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types';
import styles from './BagPanel.module.css';

interface BagItemRaw {
  id: number; propId: number; count: number; vary: string; varyname: number;
  equipPetId: number | null; zbing: number; sell: number;
  name?: string; img?: string; propsColor?: number; category?: string;
  requires?: string; effect?: string; buy?: number; yb?: number;
  usages?: string; cantrade?: number; propslock?: number; expire?: string;
  series?: string; serieseffect?: string; pluseffect?: string; prestige?: number;
  plusflag?: number; pluspid?: number; plusget?: string; plusnum?: number;
  postion?: number;
}

const PROPS_COLORS: Record<number, string> = {
  1: '#FEFDFA', 2: '#0067CB', 3: '#9833DC', 4: '#14FD10', 5: '#FED625', 6: '#ED9037',
};

const SLOT_NAMES = ['武器','衣服','头盔','鞋子','项链','戒指左','戒指右','护腕','腰带','特殊','翅膀'];

function getTradeStatus(item: BagItemRaw) {
  if (item.cantrade === 0) return item.propslock === 1 ? '可交易' : '不可交易';
  if (item.cantrade === 1) return '可交易';
  return '不可交易';
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
  const [items, setItems] = useState<BagItemRaw[]>([]);
  const [pets, setPets] = useState<PetBrief[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [useTarget, setUseTarget] = useState<BagItemRaw | null>(null);
  const [useResult, setUseResult] = useState<string | null>(null);
  const [maxBag, setMaxBag] = useState(30);
  const [tooltip, setTooltip] = useState<{ item: BagItemRaw; x: number; y: number } | null>(null);
  const setBag = useGameStore((s) => s.setBag);
  const setGamePets = useGameStore((s) => s.setPets);
  const fetchPlayer = useGameStore((s) => s.triggerRefresh);
  const closePanel = useGameStore((s) => s.setActivePanel);

  const fetchItems = () => {
    apiGet<BagItemRaw[]>('/bag').then((res: ApiResponse<BagItemRaw[]>) => {
      if (res.code === 0 && res.data) {
        setItems(res.data);
        setBag(res.data.map((item) => ({
          id: item.id, name: item.name ?? `道具#${item.propId}`,
          count: item.count, type: item.vary === 'equipment' ? 2 : 1, description: '',
        })));
      }
    });
  };

  useEffect(() => {
    Promise.all([
      apiGet<BagItemRaw[]>('/bag'),
      apiGet<PetBrief[]>('/pets'),
      apiGet<{ maxBag?: number }>('/player/me'),
    ]).then(([bagRes, petRes, playerRes]) => {
      if (bagRes.code === 0 && bagRes.data) {
        setItems(bagRes.data);
        setBag(bagRes.data.map((item) => ({
          id: item.id, name: item.name ?? `道具#${item.propId}`,
          count: item.count, type: item.vary === 'equipment' ? 2 : 1, description: '',
        })));
      }
      if (petRes.code === 0 && petRes.data) setPets(petRes.data);
      if (playerRes.code === 0 && playerRes.data?.maxBag) setMaxBag(playerRes.data.maxBag);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const filtered = items.filter(i => {
    if (i.count <= 0 || i.zbing === 1) return false;
    if (category === 0) return true;
    // Match by category name from API (backend-computed)
    const itemCat = i.category ?? '';
    const filterCat = CATEGORIES[category]?.label ?? '';
    if (!itemCat || !filterCat) return false;
    return itemCat === filterCat || filterCat.startsWith(itemCat) || itemCat.startsWith(filterCat);
  });

  const usedCells = items.filter(i => i.count > 0 && i.zbing !== 1).length;

  const getCatLabel = (item: BagItemRaw) => {
    if (!item.category) return '道具';
    const c = CATEGORIES.find(c => c.vary.length > 0 && c.label.startsWith(item.category!));
    return c?.label ?? item.category;
  };
  const selectedItem = items.find(i => i.id === selectedId);

  // PHP: only healing (hp/mp) and permanent pet stat boosts need a pet target.
  // Equipment (varyname==9) is handled by equipItem separately.
  // Chests, currency, double-exp, auto-fight scrolls, openpet are player-only.
  const PET_STAT_KEYS = ['hp:','mp:','addexp','addczl','addac','addmc','addhp','addmp','addspeed','addhits','addmiss'];
  const needsPet = (item: BagItemRaw) => {
    if (item.varyname === 9) return false; // equipment uses equipItem
    const eff = item.effect || '';
    return PET_STAT_KEYS.some(k => eff.includes(k));
  };

  const mainPetId = useAuthStore((s) => s.player?.mbid);

  const handleUse = (item: BagItemRaw) => {
    if (needsPet(item)) {
      // Permanent stat boosts and addexp — auto-target main battle pet (PHP behavior)
      const eff = item.effect || '';
      const isPermBoost = ['addczl','addac','addmc','addhp','addmp','addspeed','addhits','addmiss'].some(k => eff.includes(k));
      if (eff.includes('addexp') || isPermBoost) {
        if (!mainPetId) { alert('请先在牧场设置主战宠物！'); return; }
        doUse(item, mainPetId);
        return;
      }
      // hp/mp healing — allow choosing any pet
      if (pets.length === 0) { alert('没有可使用的宠物！'); return; }
      if (pets.length === 1) { doUse(item, pets[0].id); }
      else { setUseTarget(item); }
    } else {
      // Player-only: chests, currency, double-exp, auto-fight, openpet, expand, etc.
      doUse(item, 0);
    }
  };

  const doUse = (item: BagItemRaw, petId: number) => {
    // Optimistic update: immediately decrement count
    setItems(prev => prev.map(i => i.id === item.id ? { ...i, count: i.count - 1 } : i));
    apiPost<Record<string, unknown>>(`/bag/use/${item.id}`, { petId }).then((res: ApiResponse<Record<string, unknown>>) => {
      if (res.code === 0 && res.data) {
        const d = res.data;
        const msg = d.message as string;
        if (d.equipped) {
          setUseResult(`装备成功！${d.propName} 穿戴到 ${d.slotName}${d.replaced ? '(替换旧装备)' : ''}`);
        } else if (d.unequipped) {
          setUseResult(`已卸下装备`);
        } else if (d.type === 'healHP') setUseResult(`${item.name} 为宠物恢复了 ${d.healedHP} 点HP`);
        else if (d.type === 'healMP') setUseResult(`${item.name} 为宠物恢复了 ${d.healedMP} 点MP`);
        else if (d.type === 'exp' && d.levelUp) setUseResult(`${item.name} 使宠物升级到 Lv.${d.newLevel}！`);
        else if (d.type === 'bagExpand' || d.type === 'depotExpand') {
          setUseResult(msg ?? `扩容成功`);
          if (d.type === 'bagExpand' && d.newMaxBag) setMaxBag(d.newMaxBag as number);
        } else if (d.type === 'yuanbao') setUseResult(msg ?? `获得${d.ybGained}元宝`);
        else if (d.type === 'crystal') setUseResult(msg ?? `获得水晶`);
        else if (d.type === 'openMap') setUseResult(msg ?? '地图已解锁');
        else if (msg) setUseResult(msg);
        else setUseResult(`使用了 ${item.name}`);
        setTimeout(() => setUseResult(null), 2500);
        // Refresh from server to sync
        fetchItems();
        apiGet<PetBrief[]>('/pets').then((r) => {
          if (r.code === 0 && r.data) {
            setPets(r.data);
            setGamePets(r.data.map((p: PetBrief) => ({ ...p, hp: 0, mp: 0, atk: 0, def: 0, speed: 0, element: '金' as const, quality: 0, exp: 0 })));
          }
        });
        // Clear selection if item gone
        if (item.count <= 1) setSelectedId(null);
      } else {
        setUseResult(res.message ?? '使用失败');
        setTimeout(() => setUseResult(null), 2000);
        // Revert optimistic update on failure
        fetchItems();
      }
      setUseTarget(null);
    });
  };

  const handleSell = (item: BagItemRaw) => {
    apiPost('/bag/sell/' + item.id, { count: 1 }).then(() => { fetchItems(); fetchPlayer(); });
  };

  const handleDrop = (item: BagItemRaw) => {
    if (!confirm(`确定丢弃 ${item.name} 吗？`)) return;
    apiPost('/bag/drop/' + item.id, {}).then(() => { fetchItems(); setSelectedId(null); });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {useResult && <div className={styles.toast}>{useResult}</div>}
      {useTarget && (
        <div className={styles.selectOverlay}>
          <div className={styles.selectBox}>
            <h3>选择宠物使用「{useTarget.name}」</h3>
            <div className={styles.petList}>
              {pets.map((pet) => (
                <button key={pet.id} className={styles.petOption} onClick={() => doUse(useTarget, pet.id)}>
                  {pet.name} <small>Lv.{pet.level}</small>
                </button>
              ))}
            </div>
            <button className={styles.cancelUse} onClick={() => setUseTarget(null)}>取消</button>
          </div>
        </div>
      )}

      {/* Close button — PHP .close_btn */}
      <button className={styles.closeBtn} onClick={() => closePanel(null)} />

      {/* Bag space + category filter — PHP absolute positioned */}
      <span className={styles.space}>当前背包空间：{usedCells}/{maxBag}</span>
      <select className={styles.filter} value={category} onChange={(e) => setCategory(Number(e.target.value))}>
        {CATEGORIES.map((c, i) => (<option key={i} value={i}>{c.label}</option>))}
      </select>

      {/* Column header — PHP absolute positioned */}
      <div className={styles.colHeader}>
        <span className={styles.colIcon}>图标</span>
        <span className={styles.colName}>物品名称</span>
        <span className={styles.colType}>类型</span>
        <span className={styles.colCount}>数量</span>
      </div>

      {/* Item list — PHP absolute positioned */}
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

      {/* Action buttons — PHP absolute positioned */}
      <div className={styles.actions}>
        <button className={styles.btn} disabled={!selectedItem}
          onClick={() => selectedItem && handleUse(selectedItem)}>使用</button>
        <button className={styles.btn} disabled={!selectedItem}
          onClick={() => selectedItem && handleSell(selectedItem)}>出售</button>
        <button className={styles.btn} disabled={!selectedItem}
          onClick={() => selectedItem && handleDrop(selectedItem)}>丢弃</button>
<button className={styles.btnDepot} disabled={!selectedItem}
          onClick={() => selectedItem && alert('放入仓库功能开发中')}>放入仓库</button>
      </div>

      {/* Tooltip — PHP equipment.div() full format */}
      {tooltip && (
        <div className={styles.tooltip} style={{ left: tooltip.x + 12, top: Math.max(0, tooltip.y - 120) }}>
          <div className={styles.tipFrame}>
            <div className={styles.tipName} style={{ color: PROPS_COLORS[tooltip.item.propsColor ?? 1] ?? '#FEFDFA' }}>
              <b>{tooltip.item.name}</b>
            </div>
            <div className={styles.tipTrade}>{getTradeStatus(tooltip.item)}</div>
            <div className={styles.tipExpire}>{tooltip.item.expire ?? '永久'}</div>
            {tooltip.item.varyname === 9 && tooltip.item.postion != null && (
              <div className={styles.tipRow}><span className={styles.tipLabel}>位置：</span>{SLOT_NAMES[tooltip.item.postion] ?? '未知'}</div>
            )}
            {tooltip.item.effect && <div className={styles.tipRow}><span className={styles.tipLabel}>效果：</span>{tooltip.item.effect}</div>}
            {tooltip.item.requires && <div className={styles.tipRow}><span className={styles.tipLabel}>需要：</span>{tooltip.item.requires}</div>}
            {tooltip.item.usages && <div className={styles.tipUsage}>{tooltip.item.usages}</div>}
            {tooltip.item.series && <div className={styles.tipRow}><span className={styles.tipLabel}>套装：</span>{tooltip.item.series}{tooltip.item.serieseffect ? '（' + tooltip.item.serieseffect + '）' : ''}</div>}
            {tooltip.item.pluseffect && <div className={styles.tipRow}><span className={styles.tipLabel}>附加：</span>{tooltip.item.pluseffect}</div>}
            {tooltip.item.plusget && <div className={styles.tipRow}><span className={styles.tipLabel}>强化：</span>{tooltip.item.plusget}</div>}
            {tooltip.item.plusnum != null && tooltip.item.plusnum > 0 && <div className={styles.tipRow}><span className={styles.tipLabel}>镶嵌孔：</span>{tooltip.item.plusnum}</div>}
            {tooltip.item.prestige != null && tooltip.item.prestige > 0 && <div className={styles.tipRow}><span className={styles.tipLabel}>威望：</span>{tooltip.item.prestige}</div>}
            <div className={styles.tipSell}>售价：{tooltip.item.sell ?? 0}金{tooltip.item.buy ? ' / 买价：' + tooltip.item.buy + '金' : ''}{tooltip.item.yb ? ' / ' + tooltip.item.yb + '元宝' : ''}</div>
          </div>
        </div>
      )}
    </div>
  );
}
