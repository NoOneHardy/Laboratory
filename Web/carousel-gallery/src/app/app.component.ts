import {Component} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {ImageComponent} from "./image/image.component";
import {rotate} from "./shared/animations";
import {interval, Subscription} from "rxjs";

@Component({
  selector: 'n1h-root',
  animations: [
    rotate
  ],
  standalone: true,
  imports: [CommonModule, ImageComponent, NgOptimizedImage],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  active: Image
  auto: Subscription
  states = [
    'gallery1',
    'gallery2',
    'gallery3',
    'gallery4',
    'gallery5'
  ]

  display: Image[] = []
  concertIndex = 0
  indexOfLeftItem = 0
  indexOfRightItem = 4

  data: Image[] = [
    {
      name: 'Rain',
      url: 'https://images.pexels.com/photos/531880/pexels-photo-531880.jpeg',
      desc: 'It\'s regular water'
    },
    {
      name: 'Artificial intelligence',
      url: 'https://en.almamater.si/upload/courses/AAI_V6_11239.jpg',
      desc: 'ChatGPT'
    },
    {
      name: 'Lake',
      url: 'https://helpx.adobe.com/content/dam/help/en/photoshop/using/convert-color-image-black-white/jcr_content/main-pars/before_and_after/image-before/Landscape-Color.jpg',
      desc: 'A lot of water'
    },
    {
      name: 'Mountains',
      url: 'https://img.freepik.com/free-photo/painting-mountain-lake-with-mountain-background_188544-9126.jpg',
      desc: 'A bunch of rocks'
    },
    {
      name: 'Sunrise',
      url: 'https://cdn.pixabay.com/photo/2016/05/05/02/37/sunset-1373171_1280.jpg',
      desc: 'An internet provider in switzerland'
    },
    {
      name: 'Iris',
      url: 'https://media.istockphoto.com/id/1322220448/photo/abstract-digital-futuristic-eye.jpg?s=612x612&w=0&k=20&c=oAMmGJxyTTNW0XcttULhkp5IxfW9ZTaoVdVwI2KwK5s=',
      desc: 'The colorful part of the eye'
    }
  ]

  constructor() {
    this.auto = this.refreshSubscription()
    for (this.concertIndex; this.concertIndex < 5; this.concertIndex++) {
      this.display.push(this.data[this.concertIndex])
    }
    this.active = this.display[Math.floor((this.display.length - 1) / 2) + (this.display.length - 1) % 2]
  }

  refreshSubscription(): Subscription {
    return interval(5000).subscribe(() => {
      this.next()
    })
  }

  next() {
    this.checkBounds()
    this.display[this.indexOfLeftItem++] = this.data[this.concertIndex++]
    this.indexOfRightItem++

    if (this.indexOfLeftItem + 2 >= this.display.length) this.active = this.display[this.indexOfLeftItem + 2 - this.display.length]
    else this.active = this.display[this.indexOfLeftItem + 2]

    this.states.unshift(this.states[4])
    this.states.pop()
  }

  previous() {
    this.checkBounds()
    this.display[this.indexOfRightItem--] = this.data[this.concertIndex--]
    this.indexOfLeftItem--

    if (this.indexOfLeftItem + 2 >= this.display.length) this.active = this.display[this.indexOfLeftItem + 2 - this.display.length]
    else this.active = this.display[this.indexOfLeftItem + 2]

    this.states.push(this.states[0])
    this.states.shift()
  }

  checkBounds() {
    if (this.indexOfLeftItem == -1) this.indexOfLeftItem = this.display.length - 1
    if (this.indexOfRightItem == -1) this.indexOfRightItem = this.display.length - 1
    if (this.indexOfLeftItem == this.display.length) this.indexOfLeftItem = 0
    if (this.indexOfRightItem == this.display.length) this.indexOfRightItem = 0
    if (this.concertIndex == -1) this.concertIndex = this.data.length - 1
    if (this.concertIndex == this.data.length) this.concertIndex = 0
  }

  private timeout?: number

  onScroll(e: WheelEvent) {
    e.preventDefault()
    this.auto.unsubscribe()
    clearTimeout(this.timeout)
    if (e.deltaY > 0) {
      this.next()
    }
    if (e.deltaY < 0) {
      this.previous()
    }
    this.timeout = window.setTimeout(() => {
      this.auto = this.refreshSubscription()
    }, 5000)
  }
}

export interface Image {
  name: string
  url: string
  desc: string
}
