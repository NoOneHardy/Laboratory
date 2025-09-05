import {Component, Input} from '@angular/core';
import {NgIf, NgOptimizedImage} from "@angular/common";

@Component({
  selector: 'n1h-image',
  imports: [
    NgOptimizedImage,
    NgIf
  ],
  templateUrl: './image.component.html',
  styleUrl: './image.component.css'
})
export class ImageComponent {
  @Input() url?: string
  @Input() name?: string
  @Input() desc?: string

  constructor() {
  }
}
