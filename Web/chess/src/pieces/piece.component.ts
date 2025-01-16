import {Component} from '../component'
import {Color} from './color'
import {PieceData} from './piece-data'

export abstract class PieceComponent<T extends PieceData> extends Component<T> {
  protected readonly template: string = 'piece'

  protected readonly abstract type: string
  protected readonly color: Color

  public constructor(target: string, color: Color) {
    super(target)
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

  protected abstract projectMovement(): void
  protected abstract move(): void
  protected abstract die(): void
  protected abstract kill(): void
}
