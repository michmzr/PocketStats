import {defineStore} from "pinia";

export const useSessionStore = defineStore("session", {
  state: () => {
    return { authorized: false };
  },
  actions: {
   setAuthorizedState(authorized: boolean) {
     this.authorized = authorized
   }
  },
  getters: {
    isAuthorized():boolean  {
      return this.authorized;
    },
  },
});