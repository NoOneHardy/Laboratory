import {PieceComponent} from '../piece.component'
import {Pawn} from './pawn'
import {PieceService} from '../piece.service'
import {Color} from '../color'

export class PawnComponent extends PieceComponent<Pawn> {
  protected readonly type = 'pawn'

  public constructor(target: string, color: Color) {
    super(target, color)
  }

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

  protected override async click($event: MouseEvent): Promise<void> {
    await super.click($event)
    const data = this.getData()
    if (!data) return

    await PieceService.instance.resetSelection()
    this.data = {
      ...data,
      isActive: true
    }
    await this.compile()
  }
}