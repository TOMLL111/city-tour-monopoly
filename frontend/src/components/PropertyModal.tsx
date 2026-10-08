import { gameSocket } from '../lib/socket'
import type { BoardSpace, Player, PropertyState, RoomSnapshot } from '../lib/types'
import { useGameStore } from '../store'

interface Props {
  space: BoardSpace
  snapshot: RoomSnapshot
  me: Player
  onClose: () => void
}

export default function PropertyModal({ space, snapshot, me, onClose }: Props) {
  const setError = useGameStore((state) => state.setError)
  const property = snapshot.properties[String(space.index)] as PropertyState | undefined
  if (!property) return null
  const owner = snapshot.players.find((player) => player.id === property.ownerId)
  const mine = owner?.id === me.id
  const myTurn = snapshot.gameState.turnPlayerId === me.id
  const send = (type: 'BUILD_HOUSE' | 'SELL_HOUSE' | 'MORTGAGE_PROPERTY' | 'UNMORTGAGE_PROPERTY') => {
    try { gameSocket.send(type, { propertyIndex: space.index }) } catch (error) { setError(error instanceof Error ? error.message : '操作失败') }
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <article className="modal property-card" onMouseDown={(event) => event.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>×</button>
        <div className="deed-band" style={{ background: space.colorHex || '#607d8b' }} />
        <p className="eyebrow">编号 {space.index.toString().padStart(2, '0')}</p>
        <h2>{space.name}</h2>
        <p>{space.description}</p>
        <div className="property-status">
          <span>拥有者：{owner?.nickname || '银行'}</span>
          <span>状态：{property.mortgaged ? '已抵押' : property.houses === 5 ? '酒店' : `${property.houses} 栋房屋`}</span>
        </div>
        {space.rentTable.length > 0 && (
          <table className="rent-table">
            <tbody><tr><th>租金</th>{space.rentTable.map((rent, index) => <td key={index}><small>{index === 0 ? '基础' : index === 5 ? '酒店' : `${index} 房`}</small>{rent}</td>)}</tr></tbody>
          </table>
        )}
        {mine && myTurn && (
          <div className="modal-actions">
            {space.type === 'PROPERTY' && property.houses < 5 && <button disabled={property.mortgaged} onClick={() => send('BUILD_HOUSE')}>建房 {space.houseCost}</button>}
            {property.houses > 0 && <button onClick={() => send('SELL_HOUSE')}>卖房</button>}
            {!property.mortgaged ? <button onClick={() => send('MORTGAGE_PROPERTY')}>抵押 +{Math.floor(space.price / 2)}</button> : <button onClick={() => send('UNMORTGAGE_PROPERTY')}>赎回 -{Math.ceil(space.price * 1.1)}</button>}
          </div>
        )}
      </article>
    </div>
  )
}