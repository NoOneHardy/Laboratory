import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import {GallerySliderComponent} from "./gallery-slider/gallery-slider.component";

@Component({
    selector: 'n1h-root',
    imports: [CommonModule, RouterOutlet, GallerySliderComponent],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'image-gallery';
}
