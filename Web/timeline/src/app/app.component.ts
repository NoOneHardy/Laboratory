import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TimelineComponent} from './timeline/timeline.component'

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, TimelineComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
}
