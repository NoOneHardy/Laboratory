import Handlebars from 'handlebars'

export abstract class Component<T> {
  protected readonly abstract template: string
  protected host: HTMLElement | null = null
  private readonly _target: string | null = null
  private _data: T | null = null

  protected constructor(target: string) {
    this._target = `--${target}`
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
    this.host = document.querySelector('.' + this._target)
    if (!data || !this.host) return

    const response = await fetch(`./components/${this.template}.hbs`)
    const render = Handlebars.compile(await response.text())
    this.host.innerHTML = render(data)
    this.host?.addEventListener('click', async $event => await this.click($event))
  }

  public init(): void {
    return
  }

  protected async click($event: MouseEvent): Promise<void> {
    $event.stopPropagation()
  }
}