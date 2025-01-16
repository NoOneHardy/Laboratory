import {BoardComponent} from './board/board.component'
import {PawnComponent} from './pieces/pawn/pawn.component'
import {Color} from './pieces/color'

export abstract class GameEngine {
  public static async main(): Promise<void> {
    const board = new BoardComponent('list')

    board.data = {
      rows: [{fields: [{value: 'Hello World'}, {value: 'Hello World'}]}, {fields: [{value: 'Hello World'}, {value: 'Hello World'}]}]
    }

    await board.compile()

    const pawn = new PawnComponent('pawn', Color.BLACK)
    pawn.data = {
      isDisabled: false,
      isTouched: false,
      position: {
        x: 0,
        y: 0
      }
    }

    await pawn.compile()
  }
}

GameEngine.main().then()
