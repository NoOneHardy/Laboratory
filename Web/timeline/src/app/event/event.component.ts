import {Component, Input} from '@angular/core';
import {Item} from '../timeline/timeline.component'
import {ConnectionComponent} from '../connection/connection.component'
import {NgIf} from '@angular/common'
import {ScrollAnimationDirective} from '../shared/directives/scroll-animation.directive'

@Component({
  selector: 'app-event',
  standalone: true,
  imports: [
    ConnectionComponent,
    NgIf,
    ScrollAnimationDirective
  ],
  templateUrl: './event.component.html',
  styleUrl: './event.component.css'
})
export class EventComponent {
  @Input() item?: Item
  @Input() i?: number
}
