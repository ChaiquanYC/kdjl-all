import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './AuctionPanel.module.css';

interface AucItem {
  id: number; name: string; count: number; price: number;
  sellerName?: string; varyname?: number; timeRemaining?: number;
  expired?: boolean; type?: string; vary?: number;
}
interface BagItem { id: number; name?: string; count: number; varyname?: number; }

const CATEGORIES: [number, string][] = [
  [0,'全部'],[1,'辅助'],[2,'增益'],[3,'捕捉'],[4,'收集'],
  [5,'技能书'],[9,'装备'],[11,'宝箱'],[13,'特殊'],[14,'宠物卵'],[15,'合成'],
];

export default function AuctionPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);

  const [tab, setTab] = useState(1);
  const [filterVary, setFilterVary] = useState<number | null>(null);
  const [items, setItems] = useState<AucItem[]>([]);
  const [myItems, setMyItems] = useState<AucItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [selAuc, setSelAuc] = useState<number | null>(null);
  const [selMyAuc, setSelMyAuc] = useState<number | null>(null);
  const [buyQty, setBuyQty] = useState('1');
  const [sellId, setSellId] = useState('');
  const [sellPrice, setSellPrice] = useState('');
  const [sellQty, setSellQty] = useState('1');
  const [msg, setMsg] = useState<string | null>(null);

  const auctionType = tab === 2 ? 'sj' : tab === 3 ? 'yb' : 'gold';

  const fetchData = () => {
    if (tab === 4) {
      apiGet<AucItem[]>('/auction/my').then(r => {
        if (r.code === 0) setMyItems(r.data || []);
      });
    } else {
      const vp = filterVary && filterVary > 0 ? `&varyname=${filterVary}` : '';
      apiGet<AucItem[]>(`/auction/list?type=${auctionType}${vp}`).then(r => {
        if (r.code === 0) setItems(r.data || []);
      });
    }
    const fv = filterVary && filterVary > 0 ? `?varyname=${filterVary}` : '';
    apiGet<BagItem[]>(`/auction/bag-for-sell${fv}`).then(r => {
      if (r.code === 0) setBagItems(r.data || []);
    });
  };

  useEffect(() => { fetchData(); }, [tab, filterVary]);

  const toast = (text: string) => { setMsg(text); setTimeout(() => setMsg(null), 2500); };

  const handleBuy = () => {
    if (!selAuc) { toast('请先选择要购买的物品'); return; }
    const item = items.find(i => i.id === selAuc);
    if (!item) return;
    const qty = parseInt(buyQty) || 1;
    const feeRate = auctionType === 'yb' ? 0.05 : 0.08;
    const total = item.price * qty;
    const fee = Math.round(total * feeRate);
    const currencyName = auctionType === 'sj' ? '水晶' : auctionType === 'yb' ? '元宝' : '金币';
    if (!confirm(`购买 ${item.name} x${qty}？\n单价: ${item.price} ${currencyName}\n总价: ${total} ${currencyName}\n手续费: ${fee} (${feeRate*100}%)`)) return;
    apiPost('/auction/buy/' + selAuc, { type: auctionType, quantity: qty }).then((res: any) => {
      if (res.code === 0) { toast('购买成功！手续费 ' + (res.data?.fee || 0)); setSelAuc(null); setBuyQty('1'); fetchData(); triggerRefresh(); }
      else toast(res.message);
    }).catch((e: any) => toast(e?.response?.data?.message || '购买失败'));
  };

  const handleSell = () => {
    const id = parseInt(sellId);
    if (!id) { toast('请输入背包物品ID'); return; }
    const price = parseInt(sellPrice);
    if (!price || price <= 0) { toast('请输入有效的价格'); return; }
    const qty = parseInt(sellQty) || 1;
    const currencyName = auctionType === 'sj' ? '水晶' : auctionType === 'yb' ? '元宝' : '金币';
    const feeRate = auctionType === 'yb' ? 0.05 : 0.08;
    if (!confirm(`上架拍卖？\n物品ID: ${id} x${qty}\n单价: ${price} ${currencyName}\n${feeRate*100}%手续费\n3小时后过期`)) return;
    apiPost('/auction/sell', { bagId: id, price, type: auctionType, quantity: qty }).then((res: any) => {
      if (res.code === 0) { toast('已上架！3小时后过期'); setSellId(''); setSellPrice(''); setSellQty('1'); fetchData(); triggerRefresh(); }
      else toast(res.message);
    }).catch((e: any) => toast(e?.response?.data?.message || '上架失败'));
  };

  const handleCancel = () => {
    if (!selMyAuc) { toast('请先选择要取回的物品'); return; }
    if (!confirm('确定取回选中的拍卖物品吗？')) return;
    apiPost('/auction/cancel/' + selMyAuc).then((res: any) => {
      if (res.code === 0) { toast('已取回'); setSelMyAuc(null); fetchData(); triggerRefresh(); }
      else toast(res.message);
    }).catch((e: any) => toast(e?.response?.data?.message || '取回失败'));
  };

  const handleRenew = () => {
    if (!selMyAuc) { toast('请先选择要续拍的物品'); return; }
    if (!confirm('确定续拍选中的物品吗？（续拍3小时）')) return;
    apiPost('/auction/renew/' + selMyAuc).then((res: any) => {
      if (res.code === 0) { toast('续拍成功！'); setSelMyAuc(null); fetchData(); }
      else toast(res.message);
    }).catch((e: any) => toast(e?.response?.data?.message || '续拍失败'));
  };

  const handleWithdraw = () => {
    const pm = (player as any)?.paimoney || 0;
    const psj = (player as any)?.paisj || 0;
    const pyb = (player as any)?.paiyb || 0;
    if (pm <= 0 && psj <= 0 && pyb <= 0) { toast('没有可提取的资金'); return; }
    const parts = [];
    if (pm > 0) parts.push(`金币: ${pm}`);
    if (psj > 0) parts.push(`水晶: ${psj}`);
    if (pyb > 0) parts.push(`元宝: ${pyb}`);
    if (!confirm(`确定提取拍卖所资金吗？\n${parts.join('\n')}`)) return;
    apiPost('/auction/withdraw').then((res: any) => {
      if (res.code === 0) { toast('提取成功！'); fetchData(); triggerRefresh(); }
      else toast(res.message);
    }).catch((e: any) => toast(e?.response?.data?.message || '提取失败'));
  };

  const formatTime = (sec: number | undefined) => {
    if (sec == null) return '--';
    if (sec <= 0) return '已过期';
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return m > 0 ? `${m}分${s}秒` : `${s}秒`;
  };

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.leftBg}>
        <div className={styles.returnBtn} onClick={() => setGameView('city')} />
      </div>

      <div className={styles.rightBg}>
        {/* Tabs */}
        <ul className={styles.tabs}>
          {[1,2,3,4].map(t => {
            const tabClass = t === 1 ? styles.tab1 : t === 2 ? styles.tab2 : t === 3 ? styles.tab3 : styles.tab4;
            return (
              <li key={t} className={`${tabClass} ${tab === t ? styles.tabOn : ''}`}
                onClick={() => { setTab(t); setSelAuc(null); setSelMyAuc(null); }}>
                <a className={styles.tabLink} href="javascript:void(0)" />
              </li>
            );
          })}
        </ul>

        {/* Resource bar */}
        <div className={styles.resBar}>
          {tab !== 4 && (
            <>
              金币: {player?.money ?? 0}
              <span className={styles.resSep}>|</span>
              水晶: {player?.sj ?? 0}
              <span className={styles.resSep}>|</span>
              元宝: {player?.yb ?? 0}
            </>
          )}
          {tab === 4 && (
            <>
              待取金币: {(player as any)?.paimoney || 0}
              <span className={styles.resSep}>|</span>
              待取水晶: {(player as any)?.paisj || 0}
              <span className={styles.resSep}>|</span>
              待取元宝: {(player as any)?.paiyb || 0}
            </>
          )}
          <select className={styles.filter} value={filterVary || 0} onChange={e => setFilterVary(Number(e.target.value) || null)}>
            {CATEGORIES.map(([v, n]) => <option key={v} value={v}>{n}</option>)}
          </select>
        </div>

        <div className={styles.columns}>
          {/* Left column — Auction list or My auctions */}
          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>
                {tab === 4 ? '我的拍卖' : tab === 1 ? '金币拍卖' : tab === 2 ? '水晶拍卖' : '元宝拍卖'}
              </span>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th className={styles.thIcon}></th>
                    <th>名称</th>
                    <th>价格</th>
                    <th>剩余</th>
                    {tab !== 4 && <th>卖家</th>}
                    {tab === 4 && <th>状态</th>}
                  </tr>
                </thead>
                <tbody>
                  {tab !== 4 && items.length === 0 && (
                    <tr><td colSpan={5} className={styles.empty}>暂无拍卖</td></tr>
                  )}
                  {tab === 4 && myItems.length === 0 && (
                    <tr><td colSpan={5} className={styles.empty}>暂无拍卖物品</td></tr>
                  )}
                  {(tab !== 4 ? items : myItems).map(i => {
                    const isSel = tab === 4 ? selMyAuc === i.id : selAuc === i.id;
                    const onSel = tab === 4 ? () => setSelMyAuc(isSel ? null : i.id) : () => setSelAuc(isSel ? null : i.id);
                    return (
                      <tr key={i.id} className={`${styles.row} ${isSel ? styles.rowSel : ''}`} onClick={onSel}>
                        <td className={styles.tdIcon}>
                          {i.varyname ? <img src={`/images/ui/bag/${i.varyname}.gif`} alt="" /> : null}
                        </td>
                        <td>{i.name}{i.count > 1 ? ` x${i.count}` : ''}</td>
                        <td>{i.price}</td>
                        <td className={tab === 4 && i.expired ? styles.expired : ''}>
                          {tab === 4 && i.expired ? '已过期' : formatTime(i.timeRemaining)}
                        </td>
                        {tab !== 4 && <td>{i.sellerName ?? '?'}</td>}
                        {tab === 4 && <td>{i.expired ? <span className={styles.expired}>已过期</span> : '拍卖中'}</td>}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              {tab !== 4 ? (
                <>
                  数量: <input className={styles.inp} value={buyQty} onChange={e => setBuyQty(e.target.value.replace(/\D/g, '') || '1')} />
                  <button className={styles.btn} onClick={handleBuy}>购买</button>
                </>
              ) : (
                <>
                  <button className={styles.btn} onClick={handleCancel}>取回</button>
                  <button className={styles.btn} onClick={handleRenew}>续拍</button>
                  <button className={styles.btnWide} onClick={handleWithdraw}>提取资金</button>
                </>
              )}
            </div>
          </div>

          {/* Right column — My bag */}
          <div className={styles.column}>
            <div className={styles.colHead}>
              <span className={styles.colTitle}>我的背包</span>
            </div>
            <div className={styles.itemList}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th className={styles.thIcon}></th>
                    <th>名称</th>
                    <th>ID</th>
                    <th>数量</th>
                  </tr>
                </thead>
                <tbody>
                  {bagItems.filter(i => i.count > 0).length === 0 ? (
                    <tr><td colSpan={4} className={styles.empty}>背包空空</td></tr>
                  ) : bagItems.filter(i => i.count > 0).map(i => (
                    <tr key={i.id} className={styles.row} onClick={() => { setSellId(String(i.id)); setSellQty(String(i.count)); }}>
                      <td className={styles.tdIcon}>
                        {i.varyname ? <img src={`/images/ui/bag/${i.varyname}.gif`} alt="" /> : null}
                      </td>
                      <td>{i.name ?? '道具'}</td>
                      <td>{i.id}</td>
                      <td>{i.count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.colFoot}>
              ID: <input className={styles.inp} value={sellId} onChange={e => setSellId(e.target.value.replace(/\D/g, ''))} placeholder="ID" />
              价: <input className={styles.inp} value={sellPrice} onChange={e => setSellPrice(e.target.value.replace(/\D/g, ''))} placeholder="价格" />
              量: <input className={styles.inp} value={sellQty} onChange={e => setSellQty(e.target.value.replace(/\D/g, '') || '1')} />
              <button className={styles.btn} onClick={handleSell}>上架</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
