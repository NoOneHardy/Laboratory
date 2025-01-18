import {Component} from '../component'
import {Color} from './color'
import {Piece} from './piece'

export abstract class PieceComponent<T extends Piece> extends Component<T> {
  protected readonly template: string = 'piece'

  protected readonly abstract type: string
  protected readonly color: Color

  protected constructor(target: string, color: Color) {
    super('piece-' + target)
    this.color = color
  }

  public getData(): T & {type: string | null, color: Color | null} | null {
    const data: T | null = super.getData()
    if (!data) return null
    return {
      ...data,
      type: this.type,
      color: this.color
    }
  }

  public abstract projectMovement(): void
  public abstract move(): void
  public abstract die(): void
  public abstract kill(): void
}
