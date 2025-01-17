import {PieceComponent} from '../piece.component'
import {Pawn} from './pawn'

export class PawnComponent extends PieceComponent<Pawn> {
  protected readonly type = 'pawn'

  public move(): void {
    return // TODO: Implementation
  }

  public die(): void {
    return // TODO: Implementation
  }

  public kill(): void {
    return // TODO: Implementation
  }

  public projectMovement(): void {
    return // TODO: Implementation
  }
}