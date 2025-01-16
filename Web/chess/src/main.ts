import HandleBars from 'handlebars'

export abstract class App {
    public static async main(): Promise<void> {
        const template = await fetch('./components/piece-list.hbs')
        const compileTemplate = HandleBars.compile(await template.text())

        const data = {test: 'Hello World'}

        const list = document.getElementById('list');
        list!.innerHTML = compileTemplate(data)
    }
}

App.main().then()
