import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import './assets/main.css'

const app = createApp(App)

// 使用pinia、路由、element plus
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 添加全局变量兼容SockJS
window.global = window;

app.mount('#app') 