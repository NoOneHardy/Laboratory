import {Component} from '@angular/core';
import {NgForOf, NgIf} from '@angular/common'
import {EventComponent} from '../event/event.component'

@Component({
  selector: 'app-timeline',
  imports: [
    NgIf,
    EventComponent,
    NgForOf
  ],
  templateUrl: './timeline.component.html',
  styleUrl: './timeline.component.css'
})
export class TimelineComponent {
  data: Item[] = [
    {
      id: 'item0',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date()
    },
    {
      id: 'item1',
      name: 'Item 1',
      ort: 'Weinfelden',
      timestamp: new Date('2024-06-21')
    },
    {
      id: 'item2',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-07-21')
    },
    {
      id: 'item3',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-10-05')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    },
    {
      id: 'item4',
      name: 'Item 0',
      ort: 'Weinfelden',
      timestamp: new Date('2024-09-10')
    }
  ]
}

export interface Item {
  id: string
  name: string
  ort: string
  timestamp: Date
}
