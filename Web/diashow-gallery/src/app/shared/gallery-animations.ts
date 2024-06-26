import {animate, transition, trigger} from "@angular/animations";
import {
  focusedGallery,
  mainGallery,
  nextGallery,
  nextGallery2,
  nextGallery3,
  nextGallery4,
  previousGallery
} from "./gallery-states";

export const duration_ms = 1000

export const toMainGallery = trigger('toMainGallery', [
  nextGallery,
  focusedGallery,
  mainGallery,
  transition('nextGallery => mainGallery', [
    animate(duration_ms)
  ])
])

export const toPreviousGallery = trigger('toPreviousGallery', [
  mainGallery,
  previousGallery,
  transition('mainGallery => previousGallery', [
    animate(duration_ms)
  ])
])

export const toNextGallery2 = trigger('toNextGallery2', [
  nextGallery3,
  nextGallery2,
  transition('nextGallery3 => nextGallery2', [
    animate(duration_ms)
  ])
])

export const toNextGallery = trigger('toNextGallery', [
  nextGallery2,
  nextGallery,
  transition('nextGallery2 => nextGallery', [
    animate(duration_ms)
  ])
])

export const toNextGallery3 = trigger('toNextGallery3', [
  nextGallery4,
  nextGallery3,
  transition('nextGallery4 => nextGallery3', [
    animate(duration_ms)
  ])
])
