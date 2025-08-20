import './assets/main.css'
import 'quasar/src/css/index.sass'
import '@quasar/extras/material-symbols-rounded/material-symbols-rounded.css'

import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import {Quasar} from 'quasar'

const app = createApp(App)

app.use(Quasar, {
  plugins: {}
})
app.use(router)

app.mount('#app')
