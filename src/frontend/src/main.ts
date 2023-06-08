import {createApp} from 'vue'
import App from './App.vue'
import VueAxios from 'vue-axios'
import axios from 'axios'
import router from "@/router";
import {createPinia} from "pinia";
import {BootstrapVue3, BToastPlugin} from "bootstrap-vue-3";

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'

import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {library} from "@fortawesome/fontawesome-svg-core";
import {faSpinner, faUser} from "@fortawesome/free-solid-svg-icons";

//font awesome
library.add(faSpinner, faUser);

const app = createApp(App)
const pinia = createPinia()

// @ts-ignore
app
    .use(pinia)
    .use(VueAxios, axios)
    .use(router)
    .use(BootstrapVue3)
    .use(BToastPlugin)
    .component("font-awesome-icon", FontAwesomeIcon)
    .mount('#app')

