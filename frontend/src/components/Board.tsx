import type { BoardSpace, Player, PropertyState, RoomSnapshot } from '../lib/types'

interface Props {
  snapshot: RoomSnapshot
  onSelect: (space: BoardSpace) => void
}

function position(index: number): { gridRow: number; gridColumn: number } {
  if (index === 0) return { gridRow: 11, gridColumn: 11 }
  if (index <= 9) return { gridRow: 11, gridColumn: 11 - index }
  if (index === 10) return { gridRow: 11, gridColumn: 1 }
  if (index <= 19) return { gridRow: 21 - index, gridColumn: 1 }
  if (index === 20) return { gridRow: 1, gridColumn: 1 }
  if (index <= 29) return { gridRow: 1, gridColumn: index - 19 }
  if (index === 30) return { gridRow: 1, gridColumn: 11 }
  return { gridRow: index - 29, gridColumn: 11 }
}

function playersAt(players: Player[], index: number) {
  return players.filter((player) => player.position === index && !player.bankrupt)
}

function propertyOf(snapshot: RoomSnapshot, index: number): PropertyState | undefined {
  return snapshot.properties[String(index)]
}

export default function Board({ snapshot, onSelect }: Props) {
  const current = snapshot.players.find((player) => player.id === snapshot.gameState.turnPlayerId)
  return (
    <section className="board-wrap">
      <div className="board">
        {snapshot.board.map((space) => {
          const pos = position(space.index)
          const property = propertyOf(snapshot, space.index)
          const owner = property?.ownerId ? snapshot.players.find((player) => player.id === property.ownerId) : undefined
          const occupants = playersAt(snapshot.players, space.index)
          return (
            <button
              key={space.index}
              className={`board-space space-${space.type.toLowerCase()} ${space.index % 2 ? 'even' : ''}`}
              style={{ gridRow: pos.gridRow, gridColumn: pos.gridColumn }}
              onClick={() => onSelect(space)}
              title={space.description}
            >
              {space.colorHex && <span className="color-band" style={{ background: space.colorHex }} />}
              <span className="space-name">{space.name}</span>
              {space.price > 0 && <span className="space-price">{space.price}</span>}
              {owner && <span className="owner-mark" style={{ background: owner.id === snapshot.players[0].id ? '#e45c4a' : '#2e7f83' }}>{owner.nickname.slice(0, 1)}</span>}
              {property?.mortgaged && <span className="mortgage-mark">抵押</span>}
              {property && property.houses > 0 && <span className="houses">{property.houses === 5 ? 'H' : '●'.repeat(property.houses)}</span>}
              <span className="tokens">
                {occupants.map((player) => <i className={`token token-${snapshot.players.findIndex((item) => item.id === player.id) % 7}`} key={player.id}>{player.nickname.slice(0, 1)}</i>)}
              </span>
            </button>
          )
        })}
        <div className="board-center">
          <span className="compass">CITY TOUR</span>
          <h2>城市环游记</h2>
          <div className="dice-row">
            <span>{snapshot.gameState.diceOne || '—'}</span>
            <span>{snapshot.gameState.diceTwo || '—'}</span>
          </div>
          <p>{current ? `轮到 ${current.nickname}` : '对局准备中'}</p>
          {snapshot.gameState.lastCardTitle && <div className="card-callout"><strong>{snapshot.gameState.lastCardTitle}</strong><small>{snapshot.gameState.lastCardText}</small></div>}
        </div>
      </div>
    </section>
  )
}