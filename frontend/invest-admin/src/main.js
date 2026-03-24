import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import PaginationControls from '@shared/components/PaginationControls.vue'
import './style.css'

const app = createApp(App)
app.component('PaginationControls', PaginationControls)
app.use(router)
app.mount('#app')
