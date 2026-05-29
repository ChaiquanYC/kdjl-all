import { useEffect, useState, type ReactNode } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
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
  postion?: number; holeInfo?: string; holeInfoDesc?: string;
  effectDesc?: string; requiresDesc?: string; usagesDesc?: string;
  serieseffectDesc?: string; pluseffectDesc?: string;
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

// PHP equipment.v1.php — per-varyname tooltip templates
function TooltipContent({ item, x, y }: { item: BagItemRaw; x: number; y: number }) {
  const vn = item.varyname;
  const nameColor = PROPS_COLORS[item.propsColor ?? 1] ?? '#FEFDFA';
  const usageText = item.usagesDesc || item.usages;
  const effectText = item.effectDesc || item.effect;
  const requiresText = item.requiresDesc || item.requires;
  const plusText = item.pluseffectDesc || item.pluseffect;
  const seriesText = item.serieseffectDesc || item.serieseffect;

  const renderRows = (rows: { label?: string; value: string | number | undefined; color?: string }[]) =>
    rows.filter(r => r.value != null && r.value !== '').map((r, i) => (
      <div key={i} className={styles.tipRow}>
        {r.label && <span className={styles.tipLabel}>{r.label}：</span>}
        <span style={r.color ? { color: r.color } : undefined}>{r.value}</span>
      </div>
    ));

  let body: ReactNode;

  if (vn === 9) {
    // 装备类 — PHP zhuangbei()
    const slotName = item.postion != null ? SLOT_NAMES[item.postion] ?? '未知' : null;
    const requiresLines = requiresText ? requiresText.split('，') : [];
    const holeLines = item.holeInfoDesc ? item.holeInfoDesc.split('\n') : [];
    body = (
      <>
        {slotName && <div className={styles.tipRow}><span className={styles.tipLabel}>位置：</span>{slotName}装备{item.plusflag === 1 ? '(可强化)' : '(不可强化)'}</div>}
        {renderRows([
          { label: '效果', value: effectText, color: '#FEFDFA' },
        ])}
        {requiresLines.length > 0 && (
          <>
            <div className={styles.tipRow}><span className={styles.tipLabel}>需求：</span></div>
            {requiresLines.map((line, i) => (
              <div key={i} className={styles.tipRow} style={{ paddingLeft: 12 }}>{line}</div>
            ))}
          </>
        )}
        {renderRows([
          { label: '强化', value: item.plusget },
        ])}
        {item.plusnum != null && item.plusnum > 0 && (
          <div className={styles.tipRow}><span className={styles.tipLabel}>镶嵌孔：</span>{item.plusnum}</div>
        )}
        {holeLines.length > 0 && (
          <>
            <div className={styles.tipRow}><span className={styles.tipLabel}>已镶嵌：</span></div>
            {holeLines.map((line, i) => (
              <div key={i} className={styles.tipRow} style={{ paddingLeft: 12, color: '#14FD10' }}>{line}</div>
            ))}
          </>
        )}
        {renderRows([
          { label: '附加', value: plusText, color: '#9833DC' },
        ])}
        {item.series && (
          <div className={styles.tipRow}>
            <span className={styles.tipLabel}>套装：</span>
            <span style={{ color: '#FED625' }}>{item.series}{seriesText ? '（' + seriesText + '）' : ''}</span>
          </div>
        )}
        {usageText && <div className={styles.tipUsage}>{usageText}</div>}
        {item.prestige != null && item.prestige > 0 && (
          <div className={styles.tipRow}><span className={styles.tipLabel}>威望：</span>{item.prestige}</div>
        )}
      </>
    );
  } else if (vn === 5) {
    // 技能书类 — PHP jineng()
    body = (
      <>
        {renderRows([
          { label: '效果', value: effectText, color: '#FEFDFA' },
          { label: '附加', value: plusText, color: '#9833DC' },
        ])}
        {usageText && <div className={styles.tipUsage}>{usageText}</div>}
      </>
    );
  } else if (vn === 25) {
    // 宝石类 — PHP gam()
    body = (
      <>
        {renderRows([
          { label: '镶嵌条件', value: requiresText },
        ])}
        {usageText && <div className={styles.tipUsage}>{usageText}</div>}
      </>
    );
  } else if (vn === 22) {
    // 魔法石 — PHP inline text
    body = (
      <div className={styles.tipUsage}>
        神秘的魔法石，<span style={{ cursor: 'pointer', color: '#14FD10' }}>魔法屋的芙蕾娅</span>可以帮你使用它哦。
      </div>
    );
  } else {
    // 通用道具 — PHP daoju() (varyname 1-4,6-8,10-19,23-24,26-32)
    body = (
      <>
        {usageText ? (
          <div className={styles.tipUsage}>{usageText}</div>
        ) : effectText ? (
          <div className={styles.tipRow}><span className={styles.tipLabel}>效果：</span>{effectText}</div>
        ) : null}
      </>
    );
  }

  return (
    <div className={styles.tooltip} style={{ left: x + 12, top: Math.max(0, y - 120) }}>
      <div className={styles.tipFrame}>
        <div className={styles.tipName} style={{ color: nameColor }}>
          <b>{item.name}</b>
        </div>
        <div className={styles.tipTrade}>{getTradeStatus(item)}</div>
        <div className={styles.tipExpire}>{item.expire ?? '永久'}</div>
        {body}
        <div className={styles.tipSell}>
          售价：{item.sell ?? 0}金{item.buy ? ' / 买价：' + item.buy + '金' : ''}{item.yb ? ' / ' + item.yb + '元宝' : ''}
        </div>
      </div>
    </div>
  );
}

export default function BagPanel() {
  const [items, setItems] = useState<BagItemRaw[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
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
      apiGet<{ maxBag?: number }>('/player/me'),
    ]).then(([bagRes, playerRes]) => {
      if (bagRes.code === 0 && bagRes.data) {
        setItems(bagRes.data);
        setBag(bagRes.data.map((item) => ({
          id: item.id, name: item.name ?? `道具#${item.propId}`,
          count: item.count, type: item.vary === 'equipment' ? 2 : 1, description: '',
        })));
      }
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
  const selectedPetId = useGameStore((s) => s.selectedPetId);
  // Default to selected pet, fallback to main pet
  const activePetId = selectedPetId ?? mainPetId;

  const handleUse = (item: BagItemRaw) => {
    if (needsPet(item)) {
      // Equipment always goes to main pet
      const targetPetId = item.varyname === 9 ? (mainPetId ?? activePetId) : activePetId;
      if (!targetPetId) { alert('请先在牧场设置主战宠物！'); return; }
      doUse(item, targetPetId);
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
        const err = d.error as string;
        const msg = d.message as string;
        if (err) {
          systips(err);
          fetchItems();
          return;
        }
        if (d.equipped) {
          systips(`装备成功！${d.propName} 穿戴到 ${d.slotName}${d.replaced ? '(替换旧装备)' : ''}`);
        } else if (d.unequipped) {
          systips(`已卸下装备`);
        } else if (d.type === 'healHP') systips(`${item.name} 为宠物恢复了 ${d.healedHP} 点HP`);
        else if (d.type === 'healMP') systips(`${item.name} 为宠物恢复了 ${d.healedMP} 点MP`);
        else if (d.type === 'exp' && d.levelUp) systips(`${item.name} 使宠物升级到 Lv.${d.newLevel}！`);
        else if (d.type === 'bagExpand' || d.type === 'depotExpand') {
          systips(msg ?? `扩容成功`);
          if (d.type === 'bagExpand' && d.newMaxBag) setMaxBag(d.newMaxBag as number);
        } else if (d.type === 'yuanbao') systips(msg ?? `获得${d.ybGained}元宝`);
        else if (d.type === 'crystal') systips(msg ?? `获得水晶`);
        else if (d.type === 'openMap') systips(msg ?? '地图已解锁');
        else if (d.type === 'openPet') systips(msg ?? `恭喜获得宠物：${d.petName}！`);
        else if (msg) systips(msg);
        else systips(`使用了 ${item.name}`);
        // Refresh from server to sync
        fetchItems();
        apiGet<PetBrief[]>('/pets').then((r) => {
          if (r.code === 0 && r.data) {
            setGamePets(r.data.map((p: PetBrief) => ({ ...p, hp: 0, mp: 0, atk: 0, def: 0, speed: 0, element: '金' as const, quality: 0, exp: 0 })));
          }
        });
        // Clear selection if item gone
        if (item.count <= 1) setSelectedId(null);
      } else {
        systips(res.message ?? '使用失败');
        // Revert optimistic update on failure
        fetchItems();
      }
    }).catch((err: any) => {
      // 400 errors from backend (e.g. equipment conditions not met)
      const msg = err?.response?.data?.message;
      systips(msg || '使用失败');
      fetchItems(); // Revert optimistic update
    });
  };

  const handleSell = (item: BagItemRaw) => {
    apiPost<Record<string, unknown>>('/bag/sell/' + item.id, { count: 1 }).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data;
        systips(`出售 ${d.sold} x${d.count}，获得 ${d.goldGained} 金币`);
      } else {
        systips(res.message ?? '出售失败');
      }
      fetchItems();
      fetchPlayer();
    }).catch((err: any) => {
      systips(err?.response?.data?.message || '出售失败');
    });
  };

  const handleDrop = (item: BagItemRaw) => {
    if (!confirm(`确定丢弃 ${item.name} 吗？`)) return;
    apiPost<Record<string, unknown>>('/bag/drop/' + item.id, {}).then((res) => {
      if (res.code === 0) {
        systips(`已丢弃 ${item.name}`);
      } else {
        systips(res.message ?? '丢弃失败');
      }
      fetchItems();
      setSelectedId(null);
    }).catch((err: any) => {
      systips(err?.response?.data?.message || '丢弃失败');
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>

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

      {/* Tooltip — matches PHP equipment.div() templates per varyname */}
      {tooltip && <TooltipContent item={tooltip.item} x={tooltip.x} y={tooltip.y} />}
    </div>
  );
}
