import {Component} from '../component'
import {Board} from './board'
import {PieceService} from '../pieces/piece.service'

export class BoardComponent extends Component<Board> {
  protected template = 'board'

  constructor(target: string) {
    super(target)
  }

  public async compile(): Promise<void> {
    await super.compile()
    for (const piece of PieceService.instance.pieces) {
      await piece.compile()
    }
  }

  public init(): void {
    const data: Board = {
      rows: Array.from(Array(8)).map(() => {
        return {
          fields: Array.from(Array(8)).map(() => {
            return {
              piece: null
            }
          })
        }
      })
    }

    for (const piece of PieceService.instance.pieces) {
      const target = piece.target
      if (!target) continue

      const col = piece.getData()?.position.x ?? 0
      const row = piece.getData()?.position.y ?? 0

      data.rows[row].fields[col].piece = target
    }

    this.data = data
  }

  protected async click($event: MouseEvent): Promise<void> {
    await super.click($event)
  }
}