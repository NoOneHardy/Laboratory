import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'date-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  todayDate = new Date().getDate()
  todayMonth = new Date().getMonth() + 1
  todayYear = new Date().getFullYear()
  today = new Date(this.todayYear, this.todayMonth - 1, this.todayDate)

  activeMonth = this.todayMonth

  createDate(month: number) {
    return new Date(this.todayYear, month - 1)
  }

  getDaysOfMonth(month: number = this.todayMonth, year: number = this.todayYear): Date[] {
    return Array.from(Array(new Date(year, month, 0).getDate()).keys()).map(v => new Date(year, month - 1, v + 1))
  }

  getWeeksOfMonth(month: number = this.todayMonth, year: number = this.todayYear): Date[][] {
    const days = this.getDaysOfMonth(month, year)
    const weeks: Date[][] = []
    for (let week = 1; weeks.flat().length < days.length; week++) {
      weeks.push(this.getDaysOfWeek(week, month, year))
    }
    return weeks
  }

  getDaysOfWeek(week: number = Math.floor(this.todayDate / 7 - .125) + 1, month: number = this.todayMonth, year: number = this.todayYear): Date[] {
    const days = this.getDaysOfMonth(month, year)
    const offset = days[0].getDay() == 0 ? 6 : days[0].getDay() - 1
    return days.filter((d) => {
      return Math.floor((d.getDate() + offset) / 7 - .125) + 1 == week
    })
  }
}
