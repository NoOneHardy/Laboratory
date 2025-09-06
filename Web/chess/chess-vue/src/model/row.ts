import {Cell} from './cell'

export class Row {
  private readonly _cells: Cell[] = []
  private readonly _index: number

  constructor(index: number) {
    this._index = index
    for (let i = 0; i < 8; i++) {
      this.cells.push(new Cell(index, i))
    }
  }

  public get cells(): Cell[] {
    return this._cells
  }

  public get index(): number {
    return this._index
  }
}
