import Handlebars from 'handlebars'

export abstract class Component<T> {
  protected readonly abstract template: string
  private readonly _target: string | null = null
  private _data: T | null = null

  protected constructor(target: string) {
    this._target = target
    this.init()
  }

  public get target(): string | null {
    return this._target
  }

  public set data(data: T) {
    this._data = data
  }

  protected getData(): T | null {
    return this._data
  }

  public async compile(): Promise<void> {
    const data: T | null = this.getData()
    if (!data || !this._target) return

    const response = await fetch(`./components/${this.template}.hbs`)
    const render = Handlebars.compile(await response.text())
    const html = render(data)

    document.querySelectorAll('.--' + this._target).forEach((node) => {
      node.innerHTML = html
    })
  }

  public init(): void {
    return
  }
}