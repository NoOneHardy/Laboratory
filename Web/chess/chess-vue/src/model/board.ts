import {Row} from './row'
import {Pawn} from '@/model/pieces/pawn.ts'

export class Board {
  private readonly _rows: Row[] = []

  constructor() {
    for (let i = 0; i < 8; i++) {
      this.rows.push(new Row(i))
    }

    this.rows[0].cells[0].piece = new Pawn('white')
  }

  public get rows(): Row[] {
    return this._rows
  }
}
