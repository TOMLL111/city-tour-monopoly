import type { RoomCredentials, RoomSnapshot } from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || '请求失败')
  return data as T
}

export function createRoom(nickname: string) {
  return request<RoomCredentials>('/api/rooms', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nickname })
  })
}

export function joinRoom(roomCode: string, nickname: string) {
  return request<RoomCredentials>(`/api/rooms/${roomCode}/join`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nickname })
  })
}

export function getSnapshot(roomCode: string, playerToken: string) {
  return request<RoomSnapshot>(`/api/rooms/${roomCode}`, {
    headers: { 'X-Player-Token': playerToken }
  })
}