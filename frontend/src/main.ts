import { createApp } from 'vue'
import { createPinia } from 'pinia'
import mapboxgl from 'mapbox-gl'

import App from './App.vue'
import router from './router'

mapboxgl.accessToken = import.meta.env.VITE_MAPBOX_TOKEN as string

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
