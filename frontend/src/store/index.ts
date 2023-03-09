import {defineStore} from "pinia";
import {SyncStatusInterface} from "@/models/sync-models";

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
      } as SyncStatusInterface)
    }
  },

  actions: {
    setSyncStatus(state: SyncStatusInterface) {
      this.lastState = state;
    }
  },
  getters: {
    getSyncStatus(): SyncStatusInterface {
      return this.lastState
    }
  },
});

