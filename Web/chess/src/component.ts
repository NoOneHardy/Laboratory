import Handlebars from 'handlebars'

export abstract class Component<T> {
  protected readonly abstract template: string
  private readonly target: HTMLElement | null
  private _data: T | null = null

  protected constructor(target: string) {
    this.target = document.getElementById(target)
  }

  public set data(data: T) {
    this._data = data
  }

  protected getData(): T | null {
    return this._data
  }

  public async compile(): Promise<void> {
    const data: T | null = this.getData()
    if (!data || !this.target) return

    const response = await fetch(`./components/${this.template}.hbs`)
    const render = Handlebars.compile(await response.text())

    this.target.innerHTML = render(data)
  }
}