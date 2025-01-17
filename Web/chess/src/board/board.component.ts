import {Component} from '../component'
import {Board} from './board'

export class BoardComponent extends Component<Board> {
  protected template = 'board'

  constructor(target: string) {
    super(target)
  }

  public init(): void {
    this.data = {
      rows: Array.from(Array(8)).map(() => {
        return {
          fields: Array.from(Array(8)).map(() => {
            return {
              piece: 'pawn1'
            }
          })
        }
      })
    }

    const data = this.getData()!
    data.rows[0].fields[0].piece = 'pawn1'
  }
}