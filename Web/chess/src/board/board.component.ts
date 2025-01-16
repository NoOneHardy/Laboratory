import {Component} from "../component";
import {BoardData} from "./model/board-data";

export class BoardComponent extends Component<BoardData>{
    protected template = 'board'

    constructor(target: string) {
        super(target);
    }
}