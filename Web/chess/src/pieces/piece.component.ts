import {Component} from '../component'
import {Color} from './color'
import {PieceData} from './piece-data'

export abstract class PieceComponent<T extends PieceData> extends Component<T> {
  protected readonly template: string = 'piece'

  protected readonly abstract type: string
  protected readonly color: Color

  public constructor(target: string, color: Color) {
    super('piece-' + target)
    this.color = color
  }

  protected getData(): T & {type: string | null, color: Color | null} | null {
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
