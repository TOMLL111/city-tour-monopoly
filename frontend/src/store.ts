import { create } from 'zustand'
import type { RoomCredentials, RoomSnapshot } from './lib/types'

const STORAGE_KEY = 'city-tour-session'

function loadCredentials(): RoomCredentials | undefined {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) as RoomCredentials : undefined
  } catch {
    return undefined
  }
}

interface GameStore {
  credentials?: RoomCredentials
  snapshot?: RoomSnapshot
  connection: 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'offline'
  error?: string
  setCredentials: (credentials?: RoomCredentials) => void
  setSnapshot: (snapshot: RoomSnapshot) => void
  setConnection: (connection: GameStore['connection']) => void
  setError: (error?: string) => void
  reset: () => void
}

export const useGameStore = create<GameStore>((set) => ({
  credentials: loadCredentials(),
  snapshot: loadCredentials()?.snapshot,
  connection: 'idle',
  setCredentials: (credentials) => {
    if (credentials) localStorage.setItem(STORAGE_KEY, JSON.stringify(credentials))
    else localStorage.removeItem(STORAGE_KEY)
    set({ credentials, snapshot: credentials?.snapshot, error: undefined })
  },
  setSnapshot: (snapshot) => set({ snapshot, error: undefined }),
  setConnection: (connection) => set({ connection }),
  setError: (error) => set({ error }),
  reset: () => {
    localStorage.removeItem(STORAGE_KEY)
    set({ credentials: undefined, snapshot: undefined, connection: 'idle', error: undefined })
  }
}))