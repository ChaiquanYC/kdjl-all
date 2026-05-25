import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './SmShopPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; yb: number; sj?: number; prestige?: number;
  img?: string; varyname?: number; timelimit?: number; stime?: number;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number;
  name?: string; img?: string; varyname?: number;
}

const SUB_CATS = [
  { label: '热卖', style: 1 }, { label: '进化合成', style: 2 },
  { label: '宠物相关', style: 3 }, { label: '装备相关', style: 4 },
];

export default function SmShopPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [subCat, setSubCat] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const maxBag = player?.maxBag ?? 30;

  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=yb'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  // Filter by shop type
  const allItems = shopItems.filter(i => `${i.stime ?? 1}`.startsWith(`${subCat}`));

  // Tab 1: 元宝商店 (yb>0)
  const ybItems = allItems.filter(i => (i.yb ?? 0) > 0);
  // Tab 2: 水晶商店 (sj>0)
  const sjItems = allItems.filter(i => (i.sj ?? 0) > 0);
  // Tab 3: VIP商店 - placeholder
  const vipItems = allItems.filter(i => (i.yb ?? 0) > 0).slice(0, 5);

  const displayItems = tab === 1 ? ybItems : tab === 2 ? sjItems : vipItems;
  const currency = tab === 1 ? 'yb' : tab === 2 ? 'sj' : 'vip';
  const curLabel = tab === 1 ? '元宝' : tab === 2 ? '水晶' : 'VIP积分';
  const bagTotal = bagItems.filter(i => i.count > 0).length;

  const handleBuy = () => {
    if (!selShop) { setMsg('请先选择商品'); return; }
    if (!confirm(`确定购买 ${count}个商品？`)) return;
    apiPost('/shop/buy/' + selShop, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        setMsg(`购买了${count}个商品`);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '购买失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleSell = () => {
    if (!selBag) { setMsg('请先选择要卖出的物品'); return; }
    if (!confirm(`确定卖出 ${count}个物品？`)) return;
    apiPost('/bag/sell/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) { setMsg(`卖出成功`); apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); }); triggerRefresh(); }
      else setMsg(res.message ?? '卖出失败');
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
        <ul className={styles.tabs}>
          <li className={tab===1?styles.tabOn:''} onClick={()=>setTab(1)}><span className={styles.t1}/></li>
          <li className={tab===2?styles.tabOn:''} onClick={()=>setTab(2)}><span className={styles.t2}/></li>
          <li className={tab===3?styles.tabOn:''} onClick={()=>setTab(3)}><span className={styles.t3}/></li>
        </ul>

        <div className={styles.resBar}>
          {tab === 1 && <><img src="/images/ui/icon01.jpg" alt="" /> 元宝：{player?.yb ?? 0}</>}
          {tab === 2 && <><img src="/images/ui/icon06.jpg" alt="" /> 水晶：{player?.sj ?? 0}</>}
          {tab === 3 && <><img src="/images/ui/icon05.jpg" alt="" /> VIP：{player?.vip ?? 0}</>}
        </div>

        <div className={styles.columns}>
          <div className={styles.column}>
            <div className={styles.subCats}>
              {SUB_CATS.map(c => (
                <img key={c.style} src={`/images/ui/smshop_0${c.style}.jpg`} alt={c.label}
                  className={subCat===c.style?styles.subOn:styles.subOff}
                  onClick={() => setSubCat(c.style)} />
              ))}
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{curLabel}</th><th className={styles.thType}>属性</th></tr></thead>
                <tbody>
                  {displayItems.length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>暂无商品</td></tr>
                  ) : displayItems.map(item => (
                    <tr key={item.id} className={`${styles.row} ${selShop===item.id?styles.rowSel:''}`}
                      onClick={() => { setSelShop(item.id); setSelBag(null); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name}</td>
                      <td className={styles.tdPrice}>{tab===1?item.yb:tab===2?item.sj:item.yb}</td>
                      <td className={styles.tdType}>{item.timelimit ? `限时` : ''}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              数量：<input className={styles.numInp} type="text" value={count} onChange={e => setCount(Number(e.target.value)||1)} />
              <button className={styles.btn} onClick={handleBuy}>购买</button>
              <button className={styles.btn} onClick={handleSell}>卖出</button>
            </div>
          </div>

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
                    <tr key={item.id} className={`${styles.row} ${selBag===item.id?styles.rowSel:''}`}
                      onClick={() => { setSelBag(item.id); setSelShop(null); setCount(item.count); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                      <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                      <td className={styles.tdType}>{item.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>背包空间：{bagTotal}/{maxBag}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
