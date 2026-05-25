import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './ZbPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; prestige: number;
  img?: string; varyname?: number; requires?: string; postion?: number;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number;
  name?: string; varyname?: number;
}

const POS_FILTERS = [
  { label: '全部', pos: [] },
  { label: '武器', pos: [0] },
  { label: '衣服', pos: [1] },
  { label: '头盔', pos: [2] },
  { label: '鞋子', pos: [3] },
  { label: '其他', pos: [4,5,6,7,8,9,10,11] },
];

export default function ZbPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [posFilter, setPosFilter] = useState(0);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const maxBag = player?.maxBag ?? 30;

  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=equip'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  // Filter: varyname==9 (equipment only), buy>0 for gold, prestige>0 for prestige
  const equipItems = shopItems.filter(i => i.varyname === 9 || i.postion != null);
  const goldItems = equipItems.filter(i => (i.buy ?? 0) > 0 && (i.prestige ?? 0) === 0);
  const prestigeItems = equipItems.filter(i => (i.prestige ?? 0) > 0);

  const displayItems = (tab === 1 ? goldItems : prestigeItems).filter(i => {
    if (posFilter === 0) return true;
    return POS_FILTERS[posFilter].pos.includes(i.postion ?? -1);
  });

  const bagTotal = bagItems.filter(i => i.count > 0).length;

  const handleBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    if (!confirm(`确定购买 ${count}个 ${item.name}？`)) return;
    apiPost('/shop/buy/' + item.id, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        setMsg(`购买了${count}个 ${item.name}`);
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
      if (res.code === 0) {
        setMsg(`卖出成功，获得${res.data?.goldGained ?? 0}金币`);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '卖出失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  const curLabel = tab === 1 ? '装备商店' : '威望装备';

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.leftBg}>
        <div className={styles.returnBtn} onClick={() => setGameView('city')} />
      </div>

      <div className={styles.rightBg}>
        <ul className={styles.tabs}>
          <li className={tab === 1 ? styles.tabOn : ''} onClick={() => setTab(1)}><span className={styles.t1} /></li>
          <li className={tab === 2 ? styles.tabOn : ''} onClick={() => setTab(2)}><span className={styles.t2} /></li>
        </ul>

        <div className={styles.resBar}>
          {tab === 1 ? <><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</>
            : <><img src="/images/ui/icon02.jpg" alt="" /> 威望：{player?.prestige ?? 0}</>}
        </div>

        <div className={styles.columns}>
          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>{curLabel}</span>
              <select className={styles.posFilter} value={posFilter} onChange={e => setPosFilter(Number(e.target.value))}>
                {POS_FILTERS.map((f, i) => <option key={i} value={i}>{f.label}</option>)}
              </select>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{tab===1?'价格':'威望'}</th><th className={styles.thReq}>需求</th></tr></thead>
                <tbody>
                  {displayItems.length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>暂无装备</td></tr>
                  ) : displayItems.map(item => (
                    <tr key={item.id} className={`${styles.row} ${selShop === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelShop(item.id); setSelBag(null); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name}</td>
                      <td className={styles.tdPrice}>{tab === 1 ? item.buy : item.prestige}</td>
                      <td className={styles.tdReq}>{item.requires ?? '无'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              数量：<input className={styles.numInp} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
              <button className={styles.btn} onClick={() => selShop ? handleBuy(displayItems.find(i => i.id === selShop)!, tab === 1 ? 'money' : 'prestige') : setMsg('请先选择装备')}>购买</button>
              {tab === 1 && <button className={styles.btn} onClick={handleSell}>卖出</button>}
            </div>
          </div>

          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>我的背包</span>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>卖价</th><th className={styles.thReq}>数量</th></tr></thead>
                <tbody>
                  {bagItems.filter(i => i.count > 0).length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>背包空空</td></tr>
                  ) : bagItems.filter(i => i.count > 0).map(item => (
                    <tr key={item.id} className={`${styles.row} ${selBag === item.id ? styles.rowSel : ''}`}
                      onClick={() => { setSelBag(item.id); setSelShop(null); setCount(item.count); }}>
                      <td className={styles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                      <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                      <td className={styles.tdReq}>{item.count}</td>
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
