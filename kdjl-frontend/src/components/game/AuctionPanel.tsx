import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './AuctionPanel.module.css';

interface AucItem { id: number; name: string; count: number; price: number; sellerName?: string; varyname?: number; timeRemaining?: number; }
interface BagItem { id: number; name?: string; count: number; varyname?: number; }

export default function AuctionPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [auctionType, setAuctionType] = useState('gold');
  const [filterVary, setFilterVary] = useState<number | null>(null);
  const [items, setItems] = useState<AucItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selAuc, setSelAuc] = useState<number | null>(null);
  const [sellId, setSellId] = useState('');
  const [sellPrice, setSellPrice] = useState('100');
  const [msg, setMsg] = useState<string | null>(null);

  const CATEGORIES: [number, string][] = [[0,'全部'],[1,'辅助'],[2,'增益'],[3,'捕捉'],[4,'收集'],[5,'技能书'],[9,'装备'],[11,'宝箱'],[13,'特殊'],[14,'宠物卵'],[15,'合成']];

  const fetchItems = () => {
    const type = tab === 2 ? 'sj' : tab === 3 ? 'yb' : 'gold';
    setAuctionType(type);
    const vp = filterVary && filterVary > 0 ? `&varyname=${filterVary}` : '';
    apiGet<AucItem[]>(`/auction/list?type=${type}${vp}`).then(r => {
      if (r.code === 0 && r.data) setItems(r.data);
    }).catch(() => {});
    apiGet<BagItem[]>(`/auction/bag-for-sell${filterVary && filterVary > 0 ? '?varyname='+filterVary : ''}`).then(r => {
      if (r.code === 0 && r.data) setBagItems(r.data);
    }).catch(() => {});
  };

  useEffect(() => { fetchItems(); setLoading(false); }, [tab, filterVary]);

  const handleBuy = () => {
    if (!selAuc) { setMsg('请先选择要购买的物品'); return; }
    const item = items.find(i => i.id === selAuc);
    const feeRate = auctionType === 'yb' ? 0.05 : 0.08;
    const fee = item ? Math.round((item.price || 0) * feeRate) : 0;
    if (!confirm(`购买 ${item?.name}？\n价格: ${item?.price} ${auctionType==='sj'?'水晶':auctionType==='yb'?'元宝':'金币'}\n手续费: ${fee} (${feeRate*100}%)`)) return;
    apiPost('/auction/buy/' + selAuc, { type: auctionType }).then((res: any) => {
      if (res.code === 0) { setMsg('购买成功！手续费 ' + (res.data?.fee||0)); setSelAuc(null); fetchItems(); triggerRefresh(); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const handleSell = () => {
    if (!sellId) { setMsg('请输入背包物品ID'); return; }
    const feeInfo = auctionType === 'yb' ? '手续费5%' : '手续费8%';
    if (!confirm(`上架拍卖？\n价格: ${sellPrice} ${auctionType==='sj'?'水晶':auctionType==='yb'?'元宝':'金币'}\n${feeInfo}\n3小时后过期`)) return;
    apiPost('/auction/sell', { bagId: Number(sellId), price: Number(sellPrice), type: auctionType }).then((res: any) => {
      if (res.code === 0) { setMsg('已上架！3小时后过期'); setSellId(''); setSellPrice('100');
        fetchItems(); triggerRefresh();
        apiGet<AucItem[]>('/auction/list').then(r => { if (r.code===0 && r.data) setItems(r.data); });
        apiGet<BagItem[]>('/bag').then(r => { if (r.code===0 && r.data) setBagItems(r.data); });
      } else setMsg(res.message);
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
          <li className={tab===1?styles.tabOn:''} onClick={()=>setTab(1)}>金币拍卖</li>
          <li className={tab===2?styles.tabOn:''} onClick={()=>setTab(2)}>水晶拍卖</li>
          <li className={tab===3?styles.tabOn:''} onClick={()=>setTab(3)}>元宝拍卖</li>
          <li className={tab===4?styles.tabOn:''} onClick={()=>setTab(4)}>我的拍卖</li>
        </ul>

        <div className={styles.resBar}>
          {tab===1 && <>金币: {player?.money ?? 0}</>}
          {tab===2 && <>水晶: --</>}
          {tab===3 && <>元宝: {player?.yb ?? 0}</>}
          {tab===4 && <>我的拍卖</>}
          <select className={styles.filter} value={filterVary||0} onChange={e => setFilterVary(Number(e.target.value)||null)}>
            {CATEGORIES.map(([v,n]) => <option key={v} value={v}>{n}</option>)}
          </select>
        </div>

        <div className={styles.columns}>
          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>{tab===1?'金币拍卖':tab===2?'水晶拍卖':tab===3?'元宝拍卖':'我的拍卖'}</span>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th>名称</th><th>价格</th><th>剩余</th><th>卖家</th></tr></thead>
                <tbody>
                  {items.length===0 ? <tr><td colSpan={4} className={styles.empty}>暂无拍卖</td></tr> :
                  items.map(i => (
                    <tr key={i.id} className={`${styles.row} ${selAuc===i.id?styles.rowSel:''}`}
                      onClick={() => setSelAuc(selAuc===i.id?null:i.id)}>
                      <td>{i.name}{i.count>1?` x${i.count}`:''}</td>
                      <td>{i.price}</td>
                      <td>{i.timeRemaining ? `${Math.floor(i.timeRemaining/60)}分` : '--'}</td>
                      <td>{i.sellerName ?? '?'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              <button className={styles.btn} onClick={handleBuy}>购买</button>
            </div>
          </div>

          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>我的背包</span>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead><tr><th className={styles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>ID</th><th className={styles.thSeller}>数量</th></tr></thead>
                <tbody>
                  {bagItems.filter(i=>i.count>0).length===0 ? <tr><td colSpan={4} className={styles.empty}>背包空空</td></tr> :
                  bagItems.filter(i=>i.count>0).map(i => (
                    <tr key={i.id}><td className={styles.tdIcon}>{i.varyname ? <img src={`/images/ui/bag/${i.varyname}.gif`} alt="" /> : null}</td>
                      <td className={styles.tdName}>{i.name ?? '道具'}</td>
                      <td className={styles.tdPrice}>{i.id}</td>
                      <td className={styles.tdSeller}>{i.count}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              ID:<input className={styles.inp} value={sellId} onChange={e=>setSellId(e.target.value)} placeholder="物品ID" />
              价:<input className={styles.inp} value={sellPrice} onChange={e=>setSellPrice(e.target.value)} />
              <input className={styles.inp} value={sellPrice} onChange={e=>setSellPrice(e.target.value)} placeholder="价格" />
              <button className={styles.btn} onClick={handleSell}>上架</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
