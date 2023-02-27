import {createApp} from 'vue'
import App from './App.vue'
import VueAxios from 'vue-axios'
import axios from 'axios'
import router from "@/router";
import {createPinia} from "pinia";

const pinia = createPinia()

createApp(App)
    .use(pinia)
    .use(VueAxios, axios)
    .use(router)
    .mount('#app')
