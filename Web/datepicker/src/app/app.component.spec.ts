import {ComponentFixture, TestBed} from '@angular/core/testing';

import {AppComponent} from './app.component'

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate days for january', () => {
    const weeks = component.getWeeksOfMonth(1, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      7, 7, 7, 7, 3
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 0, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for february with 29 days', () => {
    const weeks = component.getWeeksOfMonth(2, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      4, 7, 7, 7, 4
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 1, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for february with 28 days', () => {
    const weeks = component.getWeeksOfMonth(2, 2023).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      5, 7, 7, 7, 2
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2023, 1, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for march', () => {
    const weeks = component.getWeeksOfMonth(3, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      3, 7, 7, 7, 7
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 2, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for april', () => {
    const weeks = component.getWeeksOfMonth(4, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      7, 7, 7, 7, 2
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 3, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for may', () => {
    const weeks = component.getWeeksOfMonth(5, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      5, 7, 7, 7, 5
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 4, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for june', () => {
    const weeks = component.getWeeksOfMonth(6, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      2, 7, 7, 7, 7
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 5, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for july', () => {
    const weeks = component.getWeeksOfMonth(7, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      7, 7, 7, 7, 3
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 6, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for august', () => {
    const weeks = component.getWeeksOfMonth(8, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      4, 7, 7, 7, 6
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 7, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for september', () => {
    const weeks = component.getWeeksOfMonth(9, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      1, 7, 7, 7, 7, 1
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 8, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for october', () => {
    const weeks = component.getWeeksOfMonth(10, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      6, 7, 7, 7, 4
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 9, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for november', () => {
    const weeks = component.getWeeksOfMonth(11, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      3, 7, 7, 7, 6
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 10, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for december', () => {
    const weeks = component.getWeeksOfMonth(12, 2024).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      1, 7, 7, 7, 7, 2
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2024, 11, d + 1 + offset).valueOf()))
    })
  })

  it('should calculate days for january 2025', () => {
    const weeks = component.getWeeksOfMonth(1, 2025).map(week => week.map(day => day.valueOf()))

    const dayCountPerWeek: number[] = [
      5, 7, 7, 7, 5
    ]

    expect(weeks.length).toEqual(dayCountPerWeek.length)
    weeks.forEach((week, i) => {
      const offset = i == 0 ? 0 : dayCountPerWeek.slice(0, i).reduce((p, v) => p + v)
      expect(week).toEqual(Array.from(Array(dayCountPerWeek[i]).keys()).map(d => new Date(2025, 0, d + 1 + offset).valueOf()))
    })
  })
});
