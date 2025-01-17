import {BoardComponent} from './board/board.component'

export abstract class GameEngine {
  public static async main(): Promise<void> {
    const board = new BoardComponent('board')
    await board.compile()
  }
}

GameEngine.main().then()
