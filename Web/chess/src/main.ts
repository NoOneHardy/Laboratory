import {BoardComponent} from './board/board.component'
import {PawnComponent} from './pieces/pawn/pawn.component'
import {Color} from './pieces/color'

export abstract class GameEngine {
  public static async main(): Promise<void> {
    const board = new BoardComponent('board')
    await board.compile()

    const pawn = new PawnComponent('pawn1', Color.BLACK)
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
