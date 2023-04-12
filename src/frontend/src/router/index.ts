import {createRouter, createWebHashHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import {AuthorizationService} from "@/services/authorization-service";
import {SyncService} from "@/services/sync-service";

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeResolve((to, from, next) => {
  new AuthorizationService().updateAuthorizationState();
  new SyncService().updateLastSyncStatus();
  next()
})

export default router
