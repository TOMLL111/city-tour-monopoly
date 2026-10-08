import type { CommandType, RoomCredentials, RoomSnapshot } from './types'

interface SocketCallbacks {
  onSnapshot: (snapshot: RoomSnapshot, playerId: string) => void
  onError: (message: string) => void
  onStatus: (status: 'connecting' | 'connected' | 'reconnecting' | 'offline') => void
}

export class GameSocket {
  private socket?: WebSocket
  private credentials?: RoomCredentials
  private callbacks?: SocketCallbacks
  private heartbeat?: number
  private retry?: number
  private intentionallyClosed = false

  connect(credentials: RoomCredentials, callbacks: SocketCallbacks) {
    this.disconnect(false)
    this.credentials = credentials
    this.callbacks = callbacks
    this.intentionallyClosed = false
    this.open()
  }

  private open() {
    if (!this.credentials) return
    this.callbacks?.onStatus(this.socket ? 'reconnecting' : 'connecting')
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    this.socket = new WebSocket(`${protocol}://${location.host}/ws/game`)
    this.socket.onopen = () => {
      this.callbacks?.onStatus('connected')
      this.sendRaw({
        type: 'AUTH',
        commandId: this.commandId(),
        payload: {
          roomCode: this.credentials?.roomCode,
          playerToken: this.credentials?.playerToken
        }
      })
      this.heartbeat = window.setInterval(() => {
        if (this.socket?.readyState === WebSocket.OPEN) {
          this.sendRaw({ type: 'PING', commandId: this.commandId(), payload: {} })
        }
      }, 20000)
    }
    this.socket.onmessage = (event) => {
      const message = JSON.parse(event.data)
      if (message.type === 'SNAPSHOT') this.callbacks?.onSnapshot(message.snapshot, message.playerId)
      if (message.type === 'ERROR') this.callbacks?.onError(message.message || '操作失败')
    }
    this.socket.onclose = () => {
      if (this.heartbeat) window.clearInterval(this.heartbeat)
      if (!this.intentionallyClosed) {
        this.callbacks?.onStatus('reconnecting')
        this.retry = window.setTimeout(() => this.open(), 1500)
      }
    }
    this.socket.onerror = () => this.callbacks?.onError('连接中断，正在尝试恢复')
  }

  send(type: CommandType, payload: Record<string, unknown> = {}) {
    if (this.socket?.readyState !== WebSocket.OPEN) throw new Error('连接尚未就绪')
    this.sendRaw({ type, commandId: this.commandId(), payload })
  }

  private sendRaw(message: object) {
    this.socket?.send(JSON.stringify(message))
  }

  private commandId() {
    return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`
  }

  disconnect(notify = true) {
    this.intentionallyClosed = true
    if (this.retry) window.clearTimeout(this.retry)
    if (this.heartbeat) window.clearInterval(this.heartbeat)
    if (this.socket && this.socket.readyState <= WebSocket.OPEN) this.socket.close()
    this.socket = undefined
    if (notify) this.callbacks?.onStatus('idle' as never)
  }
}

export const gameSocket = new GameSocket()