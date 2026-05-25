import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './ShopPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; yb: number; prestige: number;
  img?: string; effect?: string; varyname?: number; category?: string;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number;
  name?: string; img?: string; varyname?: number;
}

export default function ShopPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=props'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const bagTotal = bagItems.filter(i => i.count > 0).length;
  const maxBag = player?.maxBag ?? 30;

  // Tab 1: gold shop (buy>0, no yb price, no prestige)
  const goldItems = shopItems.filter(i => (i.buy ?? 0) > 0 && (i.yb ?? 0) === 0 && (i.prestige ?? 0) === 0);
  // Tab 2: prestige shop (prestige>0)
  const prestigeItems = shopItems.filter(i => (i.prestige ?? 0) > 0);

  const handleBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    const cost = (currency === 'money' ? item.buy : item.prestige) * count;
    if (!confirm(`确定购买 ${count}个 ${item.name}？共${cost}${currency==='money'?'金币':'威望'}`)) return;
    apiPost('/shop/buy/' + item.id, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        const d = res.data;
        setMsg((d?.message as string) ?? `购买了${count}个 ${item.name}`);
        // Refresh bag
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '购买失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleSell = () => {
    if (!selBag) { setMsg('请先选择要卖出的物品'); return; }
    const item = bagItems.find(i => i.id === selBag);
    if (!item) return;
    if (!confirm(`确定卖出 ${count}个 ${item.name}？共${(item.sell??0)*count}金币`)) return;
    apiPost('/bag/sell/' + item.id, { count }).then((res: any) => {
      if (res.code === 0) {
        setMsg(`卖出成功，获得${res.data?.goldGained ?? 0}金币`);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '卖出失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  const displayItems = tab === 1 ? goldItems : prestigeItems;
  const curLabel = tab === 1 ? '金币商店' : '威望商店';

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.leftBg}>
        <div className={styles.returnBtn} onClick={() => setGameView('city')} />
      </div>

      <div className={styles.rightBg}>
        {/* Tabs */}
        <ul className={styles.tabs}>
          <li className={tab === 1 ? styles.tabOn : ''} onClick={() => setTab(1)}><span className={styles.t1} /></li>
          <li className={tab === 2 ? styles.tabOn : ''} onClick={() => setTab(2)}><span className={styles.t2} /></li>
        </ul>

        {/* Gold/prestige display */}
        <div className={styles.resBar}>
          {tab === 1 ? (
            <><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</>
          ) : (
            <><img src="/images/ui/icon02.jpg" alt="" /> 威望：{player?.prestige ?? 0}</>
          )}
        </div>

        <div className={styles.columns}>
          {/* Left: Shop items */}
          <div className={styles.column}>
            <div className={styles.colTitle}>
              <img src="/images/ui/shop03.jpg" alt={curLabel} />
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{tab === 1 ? '价格' : '威望'}</th><th className={styles.thType}>属性</th></tr></thead>
                <tbody>
                  {displayItems.length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>暂无商品</td></tr>
                  ) : displayItems.map(item => (
                    <tr key={item.id} className={`${styles.row} ${selShop === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelShop(item.id); setSelBag(null); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name}</td>
                      <td className={styles.tdPrice}>{tab === 1 ? item.buy : item.prestige}</td>
                      <td className={styles.tdType}>{item.category ?? ''}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              数量：<input className={styles.numInp} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
              <button className={styles.btn} onClick={() => selShop ? handleBuy(displayItems.find(i => i.id === selShop)!, tab === 1 ? 'money' : 'prestige') : setMsg('请先选择商品')}>购买</button>
              {tab === 1 && <button className={styles.btn} onClick={handleSell}>卖出</button>}
            </div>
          </div>

          {/* Right: Player bag */}
          <div className={styles.column}>
            <div className={styles.colTitle}>
              <img src="/images/ui/icon04.jpg" alt="背包物品" />
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>卖价</th><th className={styles.thType}>数量</th></tr></thead>
                <tbody>
                  {bagItems.filter(i => i.count > 0).length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>背包空空</td></tr>
                  ) : bagItems.filter(i => i.count > 0).map(item => (
                    <tr key={item.id} className={`${styles.row} ${selBag === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelBag(item.id); setSelShop(null); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                      <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                      <td className={styles.tdType}>{item.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              背包空间：{bagTotal}/{maxBag}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
