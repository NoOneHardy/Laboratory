export interface Piece {
  position: {
    x: number
    y: number
  }
  isTouched: boolean
  isDisabled: boolean
  isActive: boolean
}