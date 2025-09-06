import {Piece} from './piece'
import {Cell} from '../cell'
import PawnComponentComponent from '../../components/PawnComponent.vue'

export class Pawn extends Piece {
  constructor(color: 'white' | 'black') {
    super(color, PawnComponentComponent)
  }

  public check(target: Cell): boolean {
    return false
  }

  move(target: Cell): Cell | null {
    return null
  }

  get possibleMoves(): Cell[] {
    return []
  }
}
