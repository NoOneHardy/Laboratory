import {AfterViewInit, Directive, ElementRef, inject, Input} from '@angular/core';
import {animate, AnimationBuilder, AnimationMetadata} from '@angular/animations'
import {hidden, visible} from '../animations/styles'

@Directive({
  selector: '[fade-scroll]',
  standalone: true
})
export class ScrollAnimationDirective implements AfterViewInit {
  @Input() delay: number = 0
  @Input() duration: number = 500

  private el = inject(ElementRef)
  private builder = inject(AnimationBuilder)
  private observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      const animation = entry.isIntersecting ? this.scrollIn() : this.scrollOut(this.duration)

      const factory = this.builder.build(animation)
      const player = factory.create(this.el.nativeElement)

      this.builder.build(this.scrollOut(0)).create(this.el.nativeElement).play()
      setTimeout(() => player.play(), this.delay)
    })
  })

  ngAfterViewInit() {
    this.observer.observe(this.el.nativeElement)
  }

  private scrollIn(): AnimationMetadata[] {
    return [
      hidden,
      animate(`${this.duration}ms ease-in-out`, visible)
    ]
  }

  private scrollOut(duration: number): AnimationMetadata[] {
    return [
      visible,
      animate(`${duration}ms ease-in-out`, hidden)
    ]
  }
}
