import { useEffect } from 'react'
import { gameSocket } from './lib/socket'
import { useGameStore } from './store'
import HomeScreen from './components/HomeScreen'
import LobbyScreen from './components/LobbyScreen'
import GameScreen from './components/GameScreen'

const statusText = {
  idle: '未连接',
  connecting: '连接中',
  connected: '已连接',
  reconnecting: '重连中',
  offline: '已离线'
}

export default function App() {
  const credentials = useGameStore((state) => state.credentials)
  const snapshot = useGameStore((state) => state.snapshot)
  const connection = useGameStore((state) => state.connection)
  const error = useGameStore((state) => state.error)
  const setSnapshot = useGameStore((state) => state.setSnapshot)
  const setConnection = useGameStore((state) => state.setConnection)
  const setError = useGameStore((state) => state.setError)
  const reset = useGameStore((state) => state.reset)

  useEffect(() => {
    if (!credentials) return
    gameSocket.connect(credentials, {
      onSnapshot: setSnapshot,
      onError: setError,
      onStatus: setConnection
    })
    return () => gameSocket.disconnect()
  }, [credentials?.roomCode, credentials?.playerToken, setConnection, setError, setSnapshot])

  const leave = () => {
    gameSocket.disconnect()
    reset()
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={leave} title="返回首页">
          <span className="brand-mark">城</span>
          <span><strong>城市环游记</strong><small>3–7 人联机大富翁</small></span>
        </button>
        {credentials && (
          <div className="topbar-meta">
            <span>房间 {credentials.roomCode}</span>
            <span className={`status-dot ${connection}`}>{statusText[connection]}</span>
            <button className="text-button danger" onClick={leave}>退出</button>
          </div>
        )}
      </header>
      {error && <div className="error-toast" onClick={() => setError(undefined)}>{error}<span>×</span></div>}
      {!credentials || !snapshot
        ? <HomeScreen />
        : snapshot.gameState.phase === 'LOBBY'
          ? <LobbyScreen />
          : <GameScreen />}
    </div>
  )
}