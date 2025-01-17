import {PieceComponent} from './piece.component'
import {PieceData} from './piece-data'
import {Color} from './color'
import {PawnComponent} from './pawn/pawn.component'

export class PieceService {
  private static _instance: PieceService
  private _pieces: PieceComponent<PieceData>[] = []

  private constructor() {
    this.init()
  }

  public static get instance(): PieceService {
    if (!PieceService._instance) {
      PieceService._instance = new PieceService()
    }
    return PieceService._instance
  }

  public get pieces(): PieceComponent<PieceData>[] {
    return this._pieces.slice()
  }

  private init(): void {
    // Pawns
    for (let i = 0; i < 8; i++) {
      const pawn = new PawnComponent(`pawn-${i}`, Color.WHITE)
      pawn.data = {
        isDisabled: false,
        isTouched: false,
        position: {x: i, y: 1}
      }
      this._pieces.push(pawn)
    }
  }
}