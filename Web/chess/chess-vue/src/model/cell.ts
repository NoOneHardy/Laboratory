import type {Piece} from '@/model/pieces/piece.ts'

export class Cell {
  private readonly _row: number
  private readonly _column: number
  private _piece: Piece | null = null

  constructor(row: number, column: number) {
    this._row = row
    this._column = column
  }

  public get row(): number {
    return this._row
  }

  public get column(): number {
    return this._column
  }

  public get id(): string {
    return `${this.row}-${this.column}`
  }

  public get piece(): Piece | null {
    return this._piece
  }

  public set piece(value: Piece | null) {
    this._piece = value
  }
}
