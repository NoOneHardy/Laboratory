import {Component} from '../component'
import {Board} from './board'

export class BoardComponent extends Component<Board>{
  protected template = 'board'

  constructor(target: string) {
    super(target)
  }
}