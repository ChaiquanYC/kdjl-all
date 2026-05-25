import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './DepotPanel.module.css';

interface ItemRaw {
  id: number; propId: number; count: number; varyname: number;
  sell: number; name?: string; img?: string; zbing?: number;
  category?: string;
}

const CATEGORIES = [
  { label: '全部道具', vary: [] },
  { label: '辅助道具', vary: [1] }, { label: '增益道具', vary: [2] }, { label: '捕捉道具', vary: [3] },
  { label: '收集道具', vary: [4] }, { label: '技能书',   vary: [5] }, { label: '卡片道具', vary: [6] },
  { label: '进化道具', vary: [7] }, { label: '合体道具', vary: [8] }, { label: '装备道具', vary: [9] },
  { label: '精练道具', vary: [10] },{ label: '宝箱道具', vary: [11] },{ label: '特殊道具', vary: [12] },
  { label: '功能道具', vary: [13] },{ label: '宠物卵',   vary: [14] },{ label: '合成道具', vary: [15] },
];

export default function DepotPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [bag, setBag] = useState<ItemRaw[]>([]);
  const [depot, setDepot] = useState<ItemRaw[]>([]);
  const [loading, setLoading] = useState(true);
  const [bagCat, setBagCat] = useState(0);
  const [depotCat, setDepotCat] = useState(0);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [selDepot, setSelDepot] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);

  const maxBag = player?.maxBag ?? 30;
  const maxDepot = 50;

  const fetchData = () => {
    setLoading(true);
    // Fetch both in series to avoid any race conditions
    apiGet<ItemRaw[]>('/bag').then(bagRes => {
      if (bagRes.code === 0 && bagRes.data) setBag(bagRes.data);
      return apiGet<ItemRaw[]>('/depot');
    }).then(depRes => {
      if (depRes.code === 0 && depRes.data) setDepot(depRes.data);
    }).catch(() => {}).finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(); }, []);

  // Filter by category name (like BagPanel fix)
  const filterByCat = (items: ItemRaw[], cat: number) => {
    if (cat === 0) return items;
    const filterLabel = CATEGORIES[cat]?.label ?? '';
    return items.filter(i => {
      const itemCat = i.category ?? '';
      return itemCat === filterLabel || filterLabel.startsWith(itemCat) || itemCat.startsWith(filterLabel);
    });
  };

  const filterBag = filterByCat(bag.filter(i => i.count > 0 && i.zbing !== 1), bagCat);
  const filterDepot = filterByCat(depot.filter(i => i.count > 0), depotCat);

  // PHP: total count, not filtered
  const bagTotal = bag.filter(i => i.count > 0 && i.zbing !== 1).length;
  const depotTotal = depot.filter(i => i.count > 0).length;

  const handleDeposit = () => {
    if (!selBag) { setMsg('请先在背包中选择物品'); return; }
    apiPost('/depot/deposit/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) { setMsg('存放成功'); fetchData(); triggerRefresh(); setSelBag(null); setCount(1); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleWithdraw = () => {
    if (!selDepot) { setMsg('请先在仓库中选择物品'); return; }
    apiPost('/depot/withdraw/' + selDepot, { count }).then((res: any) => {
      if (res.code === 0) { setMsg('取出成功'); fetchData(); triggerRefresh(); setSelDepot(null); setCount(1); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2000);
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.leftBg}>
        <div className={styles.returnBtn} onClick={() => setGameView('city')} />
      </div>

      <div className={styles.rightBg}>
        <div className={styles.topBar}>
          <div className={styles.topBtn} />
          <div className={styles.topInfo}>
            <span><img src="/images/ui/icon01.jpg" alt="" /> 元宝：{player?.yb ?? 0}</span>
            <span><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</span>
          </div>
        </div>

        <div className={styles.columns}>
          {/* Left: Warehouse */}
          <div className={styles.column}>
            <div className={styles.colHeader}>
              <img src="/images/ui/icon03.jpg" alt="仓库物品" className={styles.colIcon} />
              <span className={styles.catLabel}>分类查看</span>
              <select className={styles.catSelect} value={depotCat} onChange={e => setDepotCat(Number(e.target.value))}>
                {CATEGORIES.map((c, i) => (<option key={i} value={i}>{c.label}</option>))}
              </select>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>价格</th><th className={styles.thCount}>数量</th></tr></thead>
                <tbody>
                  {filterDepot.length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>仓库中没有物品</td></tr>
                  ) : filterDepot.map(item => (
                    <tr key={item.id} className={`${styles.row} ${selDepot === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelDepot(item.id); setSelBag(null); setCount(item.count); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                      <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                      <td className={styles.tdCount}>{item.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              <span>仓库空间：{depotTotal}/{maxDepot}</span>
              <input className={styles.numInput} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
              <button className={styles.btn} disabled={!selDepot} onClick={handleWithdraw}>取出</button>
              <button className={styles.btn} disabled>存放</button>
            </div>
          </div>

          {/* Right: Bag */}
          <div className={styles.column}>
            <div className={styles.colHeader}>
              <img src="/images/ui/icon04.jpg" alt="背包物品" className={styles.colIcon} />
              <span className={styles.catLabel}>分类查看</span>
              <select className={styles.catSelect} value={bagCat} onChange={e => setBagCat(Number(e.target.value))}>
                {CATEGORIES.map((c, i) => (<option key={i} value={i}>{c.label}</option>))}
              </select>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>价格</th><th className={styles.thCount}>数量</th></tr></thead>
                <tbody>
                  {filterBag.length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>背包中没有物品</td></tr>
                  ) : filterBag.map(item => (
                    <tr key={item.id} className={`${styles.row} ${selBag === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelBag(item.id); setSelDepot(null); setCount(item.count); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                      <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                      <td className={styles.tdCount}>{item.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              <span>背包空间：{bagTotal}/{maxBag}</span>
              <input className={styles.numInput} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
              <button className={styles.btn} disabled>取出</button>
              <button className={styles.btn} disabled={!selBag} onClick={handleDeposit}>存放</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
