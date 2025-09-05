import {Component} from '@angular/core';
import {Gallery} from "../shared/interfaces/gallery";
import {NgForOf, NgIf, NgOptimizedImage} from "@angular/common";
import {GalleryComponent} from "../gallery/gallery.component";
import {timer} from "rxjs";
import {RouterLink} from "@angular/router";
import {
  duration_ms,
  toMainGallery,
  toNextGallery,
  toNextGallery2, toNextGallery3,
  toPreviousGallery
} from "../shared/gallery-animations";

@Component({
    selector: 'n1h-gallery-slider',
    animations: [
        toMainGallery,
        toPreviousGallery,
        toNextGallery,
        toNextGallery2,
        toNextGallery3
    ],
    imports: [
        NgOptimizedImage,
        NgIf,
        NgForOf,
        GalleryComponent,
        RouterLink
    ],
    templateUrl: './gallery-slider.component.html',
    styleUrl: './gallery-slider.component.css'
})
export class GallerySliderComponent {
  galleries: Gallery[] = [
    {
      id: 0,
      name: 'Gallery 1',
      folder: 'gallery_1',
      amount: 2,
      mobile_preview: 0,
      preview: 1
    },
    {
      id: 1,
      name: 'Gallery 2',
      folder: 'gallery_2',
      amount: 2,
      mobile_preview: 0,
      preview: 1
    },
    {
      id: 2,
      name: 'Gallery 3',
      folder: 'gallery_3',
      amount: 2,
      mobile_preview: 0,
      preview: 1
    },
    {
      id: 3,
      name: 'Gallery 4',
      folder: 'gallery_4',
      amount: 2,
      mobile_preview: 0,
      preview: 1
    }
  ]

  displayGalleries: Gallery[] = []
  activeGallery?: Gallery
  id = 0

  promote = true

  constructor() {
    this.loadFirstGallery()
    timer(10000, 10000).subscribe(() => {
      this.loadNextGalleries()
      this.promote = false
      setTimeout(() => {
        this.promote = true
      }, 20)
    })
  }

  loadGalleries(id: number) {
    if (id >= this.galleries.length) return
    let index: number
    if (id + 1 == this.galleries.length) {
      index = 0
    } else {
      index = id + 1
    }

    // Main Gallery
    this.displayGalleries.push(this.galleries[id])

    // Next Galleries
    for (let i = index; i <= index + 2; i++) {
      let g = i < this.galleries.length ? i : i - this.galleries.length
      this.displayGalleries.push(this.galleries[g])
    }

    // Previous Gallery
    index = id - 1 < 0 ? this.galleries.length - 1 : id - 1
    this.displayGalleries.push(this.galleries[index])

    if (this.displayGalleries.length > 5) this.displayGalleries.splice(0, 5)

    this.id = id
  }

  loadFirstGallery() {
    this.loadGalleries(0)
    this.activeGallery = this.displayGalleries[0]
  }

  loadNextGalleries() {
    let id: number
    if (this.id + 1 == this.galleries.length) {
      id = 0
    } else {
      id = this.id + 1
    }

    this.loadGalleries(id)

    setTimeout(() => {
      this.activeGallery = this.displayGalleries[0]
    }, duration_ms)
  }
}
