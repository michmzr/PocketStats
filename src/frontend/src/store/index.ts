import {defineStore} from "pinia";
import {ISyncStatus} from "@/models/sync-models";

export interface SyncState {
  readers: Map<string, ISyncStatus>
}


export const useSyncStore = defineStore("sync", {

  state: (): SyncState => {
    return {
      readers: new Map<string, ISyncStatus>([])
    };
  },

  actions: {
    setSyncStatus(source: string, sourceSyncStatus: ISyncStatus) {
      this.readers.set(source, sourceSyncStatus);
    },
    getSyncStatus(source: string): ISyncStatus {
      if (this.readers.has(source)) { // @ts-ignore
        return this.readers.get(source);
      } else
        return {date: undefined, records: 0};
    }
  }
});

