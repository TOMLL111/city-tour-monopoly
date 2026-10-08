import { useMemo, useState } from 'react'
import { gameSocket } from '../lib/socket'
import type { BoardSpace, CommandType } from '../lib/types'
import { useGameStore } from '../store'
import Board from './Board'
import PropertyModal from './PropertyModal'
import TradeModal from './TradeModal'

export default function GameScreen() {
  const credentials = useGameStore((state) => state.credentials)!
  const snapshot = useGameStore((state) => state.snapshot)!
  const setError = useGameStore((state) => state.setError)
  const [selectedSpace, setSelectedSpace] = useState<BoardSpace>()
  const [tradeOpen, setTradeOpen] = useState(false)
  const [bid, setBid] = useState(0)
  const [chatText, setChatText] = useState('')
  const [feed, setFeed] = useState<'events' | 'chat'>('events')
  const me = snapshot.players.find((player) => player.id === credentials.playerId)!
  const game = snapshot.gameState
  const myTurn = game.turnPlayerId === me.id
  const current = snapshot.players.find((player) => player.id === game.turnPlayerId)
  const owned = useMemo(() => snapshot.board.filter((space) => snapshot.properties[String(space.index)]?.ownerId === me.id), [snapshot, me.id])

  const command = (type: CommandType, payload: Record<string, unknown> = {}) => {
    try { gameSocket.send(type, payload) } catch (error) { setError(error instanceof Error ? error.message : '操作失败') }
  }

  const sendChat = () => {
    if (!chatText.trim()) return
    command('SEND_CHAT', { text: chatText.trim() })
    setChatText('')
  }

  const activeTrade = game.trade
  const auction = game.auction
  const debt = game.debt
  const pendingSpace = game.pendingPurchaseIndex !== undefined ? snapshot.board[game.pendingPurchaseIndex] : undefined
  const winner = snapshot.players.find((player) => player.id === game.winnerId)

  return (
    <main className="game-screen">
      <Board snapshot={snapshot} onSelect={setSelectedSpace} />

      <aside className="game-sidebar">
        <section className="card turn-card">
          <div className="turn-heading">
            <span className={`pulse-dot ${game.phase.toLowerCase()}`} />
            <div><small>{game.phase === 'PAUSED' ? '对局暂停' : game.phase === 'FINISHED' ? '对局结束' : '当前回合'}</small>
              <h2>{game.phase === 'FINISHED' ? winner ? `${winner.nickname} 获胜` : '无人胜出' : current?.nickname || '等待中'}</h2></div>
          </div>
          <p>{game.phase === 'PAUSED' ? '有玩家掉线，所有玩家重新连接后自动继续。' : myTurn ? '轮到你行动。' : '请等待当前玩家完成操作。'}</p>
          <div className="action-stack">
            {myTurn && game.turnStep === 'WAIT_ROLL' && <button className="primary big" onClick={() => command('ROLL_DICE')}>掷骰子</button>}
            {myTurn && game.turnStep === 'WAIT_JAIL_DECISION' && <>
              <button onClick={() => command('JAIL_ACTION', { action: 'ROLL' })}>尝试掷双骰</button>
              <button onClick={() => command('JAIL_ACTION', { action: 'PAY' })}>支付 50 保释</button>
              {me.getOutOfJailCards > 0 && <button onClick={() => command('JAIL_ACTION', { action: 'CARD' })}>使用出狱卡</button>}
            </>}
            {myTurn && game.turnStep === 'WAIT_PURCHASE' && pendingSpace && <div className="purchase-box">
              <strong>{pendingSpace.name}</strong><span>售价 {pendingSpace.price}</span>
              <button className="primary" disabled={me.cash < pendingSpace.price} onClick={() => command('BUY_PROPERTY')}>购买</button>
              <button onClick={() => command('DECLINE_PURCHASE')}>放弃并拍卖</button>
            </div>}
            {myTurn && game.turnStep === 'WAIT_END_TURN' && <button className="primary big" onClick={() => command('END_TURN')}>结束回合</button>}
            {myTurn && (game.turnStep === 'WAIT_ROLL' || game.turnStep === 'WAIT_END_TURN') && !activeTrade && <button className="secondary" onClick={() => setTradeOpen(true)}>发起交易</button>}
            {!myTurn && game.phase === 'PLAYING' && <p className="muted">等待服务器广播最新状态…</p>}
          </div>
        </section>
        {auction && <section className="card auction-card">
          <p className="eyebrow">AUCTION</p><h3>{snapshot.board[auction.propertyIndex].name} 拍卖</h3>
          <p>当前最高：<strong>{auction.highBid || 0}</strong> {auction.highBidderId ? `· ${snapshot.players.find((player) => player.id === auction.highBidderId)?.nickname}` : ''}</p>
          <p className="muted">等待 {snapshot.players.find((player) => player.id === auction.currentBidderId)?.nickname} 操作</p>
          {auction.currentBidderId === me.id && <div className="bid-row">
            <input type="number" min={auction.highBid + 1} max={me.cash} value={bid || auction.highBid + 1} onChange={(event) => setBid(Number(event.target.value))} />
            <button className="primary" onClick={() => command('PLACE_BID', { amount: bid || auction.highBid + 1 })}>出价</button>
            <button onClick={() => command('PASS_AUCTION')}>放弃</button>
          </div>}
        </section>}

        {activeTrade && <section className="card trade-offer-card">
          <p className="eyebrow">PENDING TRADE</p><h3>{snapshot.players.find((player) => player.id === activeTrade.fromPlayerId)?.nickname} 发起交易</h3>
          <div className="trade-summary"><span>提供：{activeTrade.offeredPropertyIndexes.length} 地产 · {activeTrade.offeredCash} 现金 · {activeTrade.offeredJailCards} 出狱卡</span>
            <span>请求：{activeTrade.requestedPropertyIndexes.length} 地产 · {activeTrade.requestedCash} 现金 · {activeTrade.requestedJailCards} 出狱卡</span></div>
          {activeTrade.toPlayerId === me.id && <div className="modal-actions"><button className="primary" onClick={() => command('RESPOND_TRADE', { accept: true })}>接受</button><button onClick={() => command('RESPOND_TRADE', { accept: false })}>拒绝</button></div>}
          {activeTrade.fromPlayerId === me.id && <button onClick={() => command('CANCEL_TRADE')}>取消交易</button>}
        </section>}

        {debt && <section className="card debt-card">
          <p className="eyebrow">DEBT</p><h3>{debt.amount} 旅行币待偿还</h3><p>{debt.reason}</p>
          {debt.debtorId === me.id ? <div className="modal-actions">
            <button className="primary" disabled={me.cash < debt.amount} onClick={() => command('SETTLE_DEBT')}>结算债务</button>
            <button className="danger-button" onClick={() => command('DECLARE_BANKRUPTCY')}>宣布破产</button>
          </div> : <p className="muted">等待 {snapshot.players.find((player) => player.id === debt.debtorId)?.nickname} 处理债务。</p>}
        </section>}

        <section className="card player-list-card">
          <div className="section-title"><h3>玩家资产</h3><span>{snapshot.players.length}/7</span></div>
          <div className="player-list">{snapshot.players.map((player, index) => <div className={`player-row ${player.id === game.turnPlayerId ? 'current' : ''} ${player.bankrupt ? 'bankrupt' : ''}`} key={player.id}>
            <i className={`token token-${index}`}>{player.nickname.slice(0, 1)}</i>
            <div><strong>{player.nickname}{player.id === me.id ? '（你）' : ''}</strong><small>{player.bankrupt ? '已破产' : `${player.cash} 旅行币 · ${player.inJail ? '监狱中' : '自由行动'}`}</small></div>
            <span>{snapshot.board.filter((space) => snapshot.properties[String(space.index)]?.ownerId === player.id).length} 地</span>
          </div>)}</div>
        </section>

        <section className="card own-assets">
          <div className="section-title"><h3>我的地产</h3><span>{owned.length}</span></div>
          <div className="asset-chips">{owned.length ? owned.map((space) => <button key={space.index} onClick={() => setSelectedSpace(space)}>
            <i style={{ background: space.colorHex || '#78909c' }} />{space.name}{snapshot.properties[String(space.index)].houses > 0 ? ` · ${snapshot.properties[String(space.index)].houses === 5 ? '酒店' : snapshot.properties[String(space.index)].houses + '房'}` : ''}
          </button>) : <p className="muted">尚未购买地产</p>}</div>
        </section>

        <section className="card feed-card">
          <div className="feed-tabs">
            <button className={feed === 'events' ? 'active' : ''} onClick={() => setFeed('events')}>事件</button>
            <button className={feed === 'chat' ? 'active' : ''} onClick={() => setFeed('chat')}>聊天</button>
          </div>
          <div className="feed-list">
            {feed === 'events' ? snapshot.events.slice(-30).reverse().map((event) => <p key={event.sequence}><time>{new Date(event.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}</time>{event.message}</p>)
              : snapshot.chatMessages.slice(-30).reverse().map((message) => <p className={message.type === 'EMOTE' ? 'emote' : ''} key={message.sequence}><strong>{message.playerName}</strong>{message.text}</p>)}
          </div>
          {feed === 'chat' && <div className="chat-compose">
            <input maxLength={200} value={chatText} onChange={(event) => setChatText(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && sendChat()} placeholder="发送消息…" />
            <button onClick={sendChat}>发送</button>
            <div className="emotes">{['👋', '😂', '😮', '😭', '👍', '🤝', '🎲', '💰'].map((emote) => <button key={emote} onClick={() => command('SEND_EMOTE', { text: emote })}>{emote}</button>)}</div>
          </div>}
        </section>
      </aside>

      {selectedSpace && <PropertyModal space={selectedSpace} snapshot={snapshot} me={me} onClose={() => setSelectedSpace(undefined)} />}
      {tradeOpen && <TradeModal snapshot={snapshot} me={me} onClose={() => setTradeOpen(false)} />}
      {game.phase === 'FINISHED' && <div className="winner-overlay"><div><span className="winner-crown">★</span><p className="eyebrow">GAME OVER</p><h1>{winner ? `${winner.nickname} 获胜` : '本局无人胜出'}</h1><p>本局数据仅保存在服务器内存中。</p></div></div>}
    </main>
  )
}
