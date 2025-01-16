import {BoardComponent} from './board/board.component'

export abstract class App {
  public static async main(): Promise<void> {
    const board = new BoardComponent('list')

    board.data = {
      rows: [{fields: [{value: 'Hello World'}, {value: 'Hello World'}]}, {fields: [{value: 'Hello World'}, {value: 'Hello World'}]}]
    }
    await board.compile()
  }
}

App.main().then()
