import {defineStore} from "pinia";
import {ISyncStatus} from "@/models/sync-models";

export const useSessionStore = defineStore("session", {
  state: () => {
    return {
      authorized: false
    };
  },
  actions: {
    setSyncStatus(authorized: boolean) {
      this.authorized = authorized
    }
  },
  getters: {
    isAuthorized(): boolean {
      return this.authorized;
    }
  },
});

export const useSyncStore = defineStore("sync", {
  state: () => {
    return {
      lastState: ({
        date: undefined,
        records: 0
      } as ISyncStatus)
    }
  },

  actions: {
    setSyncStatus(state: ISyncStatus) {
      this.lastState = state;
    }
  },
  getters: {
    getSyncStatus(): ISyncStatus {
      return this.lastState
    }
  },
});

