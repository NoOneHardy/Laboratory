import {PieceComponent} from '../piece.component'
import {Pawn} from './pawn'

export class PawnComponent extends PieceComponent<Pawn> {
  protected readonly type = 'pawn'

  protected move(): void {
    return // TODO: Implementation
  }

  protected die(): void {
    return // TODO: Implementation
  }

  protected kill(): void {
    return // TODO: Implementation
  }

  protected projectMovement(): void {
    return // TODO: Implementation
  }
}