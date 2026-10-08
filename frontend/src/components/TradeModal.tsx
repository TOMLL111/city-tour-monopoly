import { useMemo, useState } from 'react'
import { gameSocket } from '../lib/socket'
import type { Player, RoomSnapshot } from '../lib/types'
import { useGameStore } from '../store'

interface Props { snapshot: RoomSnapshot; me: Player; onClose: () => void }

function PropertyPicker({ snapshot, ownerId, selected, onToggle }: {
  snapshot: RoomSnapshot; ownerId: string; selected: number[]; onToggle: (index: number) => void
}) {
  const items = snapshot.board.filter((space) => snapshot.properties[String(space.index)]?.ownerId === ownerId)
  if (!items.length) return <p className="muted">暂无可交易地产</p>
  return <div className="property-picker">{items.map((space) => {
    const property = snapshot.properties[String(space.index)]
    return <label key={space.index} className={property.houses > 0 ? 'disabled' : ''}>
      <input type="checkbox" disabled={property.houses > 0} checked={selected.includes(space.index)} onChange={() => onToggle(space.index)} />
      <span className="mini-color" style={{ background: space.colorHex || '#78909c' }} />{space.name}{property.mortgaged ? '（抵押）' : ''}
    </label>
  })}</div>
}

export default function TradeModal({ snapshot, me, onClose }: Props) {
  const candidates = snapshot.players.filter((player) => player.id !== me.id && !player.bankrupt)
  const [targetId, setTargetId] = useState(candidates[0]?.id || '')
  const [offered, setOffered] = useState<number[]>([])
  const [requested, setRequested] = useState<number[]>([])
  const [offeredCash, setOfferedCash] = useState(0)
  const [requestedCash, setRequestedCash] = useState(0)
  const [offeredCards, setOfferedCards] = useState(0)
  const [requestedCards, setRequestedCards] = useState(0)
  const target = useMemo(() => candidates.find((player) => player.id === targetId), [candidates, targetId])
  const setError = useGameStore((state) => state.setError)

  const toggle = (values: number[], setValues: (items: number[]) => void, index: number) => {
    setValues(values.includes(index) ? values.filter((item) => item !== index) : [...values, index])
  }

  const send = () => {
    if (!target) return
    try {
      gameSocket.send('PROPOSE_TRADE', {
        toPlayerId: target.id,
        offeredPropertyIndexes: offered,
        requestedPropertyIndexes: requested,
        offeredCash: Number(offeredCash) || 0,
        requestedCash: Number(requestedCash) || 0,
        offeredJailCards: Number(offeredCards) || 0,
        requestedJailCards: Number(requestedCards) || 0
      })
      onClose()
    } catch (error) { setError(error instanceof Error ? error.message : '无法发起交易') }
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <article className="modal trade-modal" onMouseDown={(event) => event.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>×</button>
        <p className="eyebrow">PLAYER TRADE</p><h2>发起交易</h2>
        <label className="field-row">交易对象<select value={targetId} onChange={(event) => { setTargetId(event.target.value); setRequested([]); setRequestedCash(0); setRequestedCards(0) }}>
          {candidates.map((player) => <option key={player.id} value={player.id}>{player.nickname}</option>)}
        </select></label>
        <div className="trade-columns">
          <section><h3>你提供</h3><PropertyPicker snapshot={snapshot} ownerId={me.id} selected={offered} onToggle={(index) => toggle(offered, setOffered, index)} />
            <label>现金 <input type="number" min={0} max={me.cash} value={offeredCash} onChange={(event) => setOfferedCash(Number(event.target.value))} /></label>
            <label>出狱卡 <input type="number" min={0} max={me.getOutOfJailCards} value={offeredCards} onChange={(event) => setOfferedCards(Number(event.target.value))} /></label>
          </section>
          <section><h3>你请求</h3>{target && <PropertyPicker snapshot={snapshot} ownerId={target.id} selected={requested} onToggle={(index) => toggle(requested, setRequested, index)} />}
            <label>现金 <input type="number" min={0} max={target?.cash || 0} value={requestedCash} onChange={(event) => setRequestedCash(Number(event.target.value))} /></label>
            <label>出狱卡 <input type="number" min={0} max={target?.getOutOfJailCards || 0} value={requestedCards} onChange={(event) => setRequestedCards(Number(event.target.value))} /></label>
          </section>
        </div>
        <div className="modal-actions"><button className="secondary" onClick={onClose}>取消</button><button className="primary" disabled={!target} onClick={send}>发送交易</button></div>
      </article>
    </div>
  )
}