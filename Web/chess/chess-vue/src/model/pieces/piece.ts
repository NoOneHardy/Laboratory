import type {Cell} from '@/model/cell.ts'
import type {Component} from 'vue'

export abstract class Piece {
  private readonly _color: 'white' | 'black'
  private readonly _component: Component
  private _position: Cell | null = null
  private _hasMoved = false

  constructor(color: 'white' | 'black', component: Component) {
    this._color = color
    this._component = component
  }

  public get component(): Component {
    return this._component
  }

  public get color(): 'white' | 'black' {
    return this._color
  }

  public get position(): Cell | null {
    return this._position
  }

  public set position(value: Cell | null) {
    this._position = value
  }

  public die(): void {
    this.position = null
  }

  public get isDead(): boolean {
    return this.position === null
  }

  public get hasMoved(): boolean {
    return this._hasMoved
  }

  public abstract get possibleMoves(): Cell[]

  public abstract check(target: Cell): boolean
  public abstract move(target: Cell): Cell | null
}
