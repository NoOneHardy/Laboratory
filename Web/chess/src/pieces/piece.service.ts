import {PieceComponent} from './piece.component'
import {Piece} from './piece'
import {Color} from './color'
import {PawnComponent} from './pawn/pawn.component'

export class PieceService {
  private static _instance: PieceService
  private _pieces: PieceComponent<Piece>[] = []

  private constructor() {
    this.init()
  }

  public static get instance(): PieceService {
    if (!PieceService._instance) {
      PieceService._instance = new PieceService()
    }
    return PieceService._instance
  }

  public get pieces(): PieceComponent<Piece>[] {
    return this._pieces.slice()
  }

  private init(): void {
    // Pawns
    for (let i = 0; i < 8; i++) {
      const pawn = new PawnComponent(`pawn-${i}`, Color.WHITE)
      pawn.data = {
        isDisabled: false,
        isTouched: false,
        isActive: false,
        position: {x: i, y: 1}
      }
      this._pieces.push(pawn)
    }
  }

  public async resetSelection(): Promise<void> {
    for (const p of this._pieces) {
      const data = p.getData()
      if (!data) continue

      p.data = {
        ...data,
        isActive: false
      }
      await p.compile()
    }
  }
}