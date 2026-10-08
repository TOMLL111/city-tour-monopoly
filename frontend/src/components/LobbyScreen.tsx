import { useState } from 'react'
import { gameSocket } from '../lib/socket'
import { useGameStore } from '../store'

export default function LobbyScreen() {
  const credentials = useGameStore((state) => state.credentials)!
  const snapshot = useGameStore((state) => state.snapshot)!
  const setError = useGameStore((state) => state.setError)
  const [copied, setCopied] = useState(false)
  const me = snapshot.players.find((player) => player.id === credentials.playerId)!
  const allReady = snapshot.players.length >= 3 && snapshot.players.every((player) => player.ready)

  const command = (type: Parameters<typeof gameSocket.send>[0], payload: Record<string, unknown> = {}) => {
    try { gameSocket.send(type, payload) } catch (error) { setError(error instanceof Error ? error.message : '操作失败') }
  }

  const copyCode = async () => {
    await navigator.clipboard.writeText(credentials.roomCode)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1500)
  }

  return (
    <main className="lobby-screen">
      <section className="lobby-head card">
        <div>
          <p className="eyebrow">好友房间</p>
          <div className="room-code-row">
            <h1>{credentials.roomCode}</h1>
            <button onClick={copyCode}>{copied ? '已复制' : '复制房间码'}</button>
          </div>
          <p className="muted">将房间码发给 2–6 位好友，所有玩家准备后由房主开始。</p>
        </div>
        <div className="lobby-actions">
          {me.host && (
            <label className="inline-field">回合时限
              <select value={snapshot.gameState.settings.turnTimeLimitSeconds}
                onChange={(event) => command('SET_SETTINGS', { turnTimeLimitSeconds: Number(event.target.value) })}>
                <option value={0}>关闭</option><option value={30}>30 秒</option><option value={60}>60 秒</option><option value={120}>120 秒</option>
              </select>
            </label>
          )}
          <button className={me.ready ? 'secondary' : 'primary'} onClick={() => command('SET_READY', { ready: !me.ready })}>
            {me.ready ? '取消准备' : '准备'}
          </button>
          {me.host && <button className="primary" disabled={!allReady} onClick={() => command('START_GAME')}>开始游戏</button>}
        </div>
      </section>

      <section className="lobby-grid">
        {snapshot.players.map((player, index) => (
          <article className={`player-seat card ${player.ready ? 'ready' : ''}`} key={player.id}>
            <span className="seat-number">0{index + 1}</span>
            <span className={`token token-${index % 7}`}>{player.nickname.slice(0, 1).toUpperCase()}</span>
            <div><strong>{player.nickname}{player.id === credentials.playerId ? '（你）' : ''}</strong>
              <small>{player.host ? '房主' : player.ready ? '已准备' : '等待准备'} · {player.connected ? '在线' : '暂时离线'}</small>
            </div>
            {me.host && !player.host && <button className="text-button danger" onClick={() => command('KICK_PLAYER', { playerId: player.id })}>移除</button>}
          </article>
        ))}
        {Array.from({ length: Math.max(0, 3 - snapshot.players.length) }, (_, index) => (
          <article className="player-seat card empty" key={`empty-${index}`}><span>等待玩家加入…</span></article>
        ))}
      </section>

      <section className="card lobby-rules">
        <h2>开局提示</h2>
        <p>至少 3 人、至多 7 人。每位玩家初始拥有 1500 旅行币；经过起点获得 200。掉线会立即暂停整局，主动退出视为破产。</p>
        <div className="event-mini-list">
          {snapshot.events.slice(-5).reverse().map((event) => <span key={event.sequence}>{event.message}</span>)}
        </div>
      </section>
    </main>
  )
}