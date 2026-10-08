export type GamePhase = 'LOBBY' | 'PLAYING' | 'PAUSED' | 'FINISHED'
export type TurnStep = 'WAIT_ROLL' | 'WAIT_JAIL_DECISION' | 'WAIT_PURCHASE' | 'AUCTION' | 'WAIT_END_TURN' | 'DEBT_RESOLUTION' | 'TRADE' | 'FINISHED'
export type SpaceType = 'GO' | 'PROPERTY' | 'RAILROAD' | 'UTILITY' | 'CHANCE' | 'FATE' | 'TAX' | 'JAIL' | 'FREE_PARKING' | 'GO_TO_JAIL'

export interface Player {
  id: string
  nickname: string
  host: boolean
  ready: boolean
  connected: boolean
  bankrupt: boolean
  position: number
  cash: number
  jailTurns: number
  inJail: boolean
  getOutOfJailCards: number
}

export interface BoardSpace {
  index: number
  name: string
  type: SpaceType
  colorGroup?: string
  colorHex?: string
  price: number
  houseCost: number
  rentTable: number[]
  taxAmount: number
  description: string
}

export interface PropertyState {
  index: number
  ownerId?: string
  mortgaged: boolean
  houses: number
}

export interface AuctionState {
  propertyIndex: number
  currentBidderId: string
  highBid: number
  highBidderId?: string
  activeBidderIds: string[]
}

export interface TradeOffer {
  id: string
  fromPlayerId: string
  toPlayerId: string
  offeredPropertyIndexes: number[]
  requestedPropertyIndexes: number[]
  offeredCash: number
  requestedCash: number
  offeredJailCards: number
  requestedJailCards: number
  resumeTurnStep: TurnStep
  status: string
}

export interface DebtState {
  debtorId: string
  creditorId?: string
  amount: number
  reason: string
}

export interface GameState {
  phase: GamePhase
  turnStep: TurnStep
  settings: { turnTimeLimitSeconds: number }
  turnPlayerId?: string
  version: number
  diceOne: number
  diceTwo: number
  doublesCount: number
  lastRollWasDoubles: boolean
  pendingPurchaseIndex?: number
  auction?: AuctionState
  trade?: TradeOffer
  debt?: DebtState
  lastCardTitle?: string
  lastCardText?: string
  winnerId?: string
  pendingAuctionIndexes: number[]
}

export interface RoomEvent {
  sequence: number
  timestamp: string
  type: string
  actorId?: string
  message: string
  amount?: number
}

export interface ChatMessage {
  sequence: number
  timestamp: string
  playerId: string
  playerName: string
  type: 'TEXT' | 'EMOTE'
  text: string
}

export interface RoomSnapshot {
  roomCode: string
  hostId: string
  version: number
  players: Player[]
  board: BoardSpace[]
  properties: Record<string, PropertyState>
  gameState: GameState
  events: RoomEvent[]
  chatMessages: ChatMessage[]
}

export interface RoomCredentials {
  roomCode: string
  playerId: string
  playerToken: string
  snapshot: RoomSnapshot
}

export type CommandType =
  | 'SET_READY' | 'SET_SETTINGS' | 'START_GAME' | 'KICK_PLAYER'
  | 'ROLL_DICE' | 'BUY_PROPERTY' | 'DECLINE_PURCHASE'
  | 'PLACE_BID' | 'PASS_AUCTION' | 'END_TURN'
  | 'BUILD_HOUSE' | 'SELL_HOUSE' | 'MORTGAGE_PROPERTY' | 'UNMORTGAGE_PROPERTY'
  | 'JAIL_ACTION' | 'PROPOSE_TRADE' | 'RESPOND_TRADE' | 'CANCEL_TRADE'
  | 'SETTLE_DEBT' | 'DECLARE_BANKRUPTCY' | 'SEND_CHAT' | 'SEND_EMOTE' | 'LEAVE_ROOM'