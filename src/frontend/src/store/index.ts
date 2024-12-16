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
      lastState: {
        READER: ({
          date: undefined,
          records: 0
        } as ISyncStatus),
        POCKET: ({
          date: undefined,
          records: 0
        } as ISyncStatus)
      }  as { [key: string]: ISyncStatus }
    }
  },

  actions: {
      setSyncStatus(source: string, state: ISyncStatus) {
      this.lastState[`${source}`] = state;
    }
  },
  getters: {
    getSyncStatus(source): ISyncStatus {
      return this.lastState[`${source}`]
    }
  },
});

