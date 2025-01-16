import Handlebars from 'handlebars'

export abstract class Component<T> {
    protected readonly target: string
    protected readonly template: string | null = null
    private _data: T | null = null

    protected constructor(target: string) {
        this.target = target
    }

    public set data(data: T) {
        this._data = data
    }

    public async compile(): Promise<void> {
        if (!this._data || !this.template) return

        const template = await fetch(`./components/${this.template}.hbs`)
        const compile = Handlebars.compile(await template.text())

        const target = document.getElementById(this.target)
        if (target) target.innerHTML = compile(this._data)
    }
}