import { useState } from 'react'
import { createRoom, joinRoom } from '../lib/api'
import { useGameStore } from '../store'

export default function HomeScreen() {
  const [nickname, setNickname] = useState('')
  const [roomCode, setRoomCode] = useState('')
  const [busy, setBusy] = useState(false)
  const setCredentials = useGameStore((state) => state.setCredentials)
  const setError = useGameStore((state) => state.setError)

  const run = async (action: () => Promise<Awaited<ReturnType<typeof createRoom>>>) => {
    if (!nickname.trim()) return setError('请先输入昵称')
    setBusy(true)
    setError(undefined)
    try {
      setCredentials(await action())
    } catch (error) {
      setError(error instanceof Error ? error.message : '操作失败')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="home-screen">
      <section className="hero-panel">
        <p className="eyebrow">CITY TOUR · 城市旅行棋盘</p>
        <h1>把好友拉进同一座城市</h1>
        <p className="hero-copy">买房、收租、拍卖和交易。所有骰子与资产由服务器判定，不使用官方棋盘素材。</p>
        <div className="feature-strip">
          <span>3–7 人</span><span>6 位房间码</span><span>断线自动暂停</span><span>完整核心规则</span>
        </div>
      </section>

      <section className="home-actions">
        <div className="card form-card">
          <label>你的昵称</label>
          <input maxLength={16} value={nickname} onChange={(event) => setNickname(event.target.value)} placeholder="最多 16 个字符" />
          <button className="primary" disabled={busy} onClick={() => run(() => createRoom(nickname.trim()))}>创建房间</button>
        </div>
        <div className="card form-card">
          <label>加入好友房间</label>
          <div className="join-row">
            <input maxLength={6} value={roomCode} onChange={(event) => setRoomCode(event.target.value.toUpperCase())} placeholder="6 位房间码" />
            <button disabled={busy || roomCode.length !== 6} onClick={() => run(() => joinRoom(roomCode, nickname.trim()))}>加入</button>
          </div>
          <p className="muted">昵称只用于本局，刷新页面会自动尝试重连。房间数据保存在服务器内存中。</p>
        </div>
      </section>
    </main>
  )
}