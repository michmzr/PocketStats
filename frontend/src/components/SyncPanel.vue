<template>
  <div class="card" v-if="authorized && loadedData">
    <div class="card-body">
      <strong>Last sync:</strong> {{ lastSyncMsg }}

      <b-button variant="link" :disabled="syncing ? true : false"
                size="sm" v-on:click="sync">
        <span v-if="!syncing">
          <font-awesome-icon icon="spinner"/>
          Sync
        </span>
        <span v-if="syncing">
          <b-spinner small type="grow"></b-spinner>
           Loading
        </span>
      </b-button>
    </div>
  </div>
</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {useSessionStore, useSyncStore} from "@/store";
import {SyncStatus} from "@/models/sync-models";
import {SyncService} from "@/services/sync-service";
import {formatRelative} from "date-fns";


export default class SyncPanel extends Vue {
  authorized: Boolean = false;
  sessionStore = useSessionStore();
  syncStore = useSyncStore();
  syncing: boolean = false;
  loadedData: boolean = false;

  lastSync: SyncStatus | undefined = undefined;
  lastSyncMsg: string | undefined;
  syncService: SyncService = new SyncService();

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized;
  }

  mounted() {
    this.loadedData = false;

    this.isAuthorized();

    this.lastSync = this.syncStore.getSyncStatus;
    this.lastSyncMsg = this.genSyncMsg();

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized;
    });

    this.syncStore.$subscribe((mutation, state) => {
      this.syncing = false;
      this.lastSync = state.lastState
      this.lastSyncMsg = this.genSyncMsg();
      this.loadedData = true;
    });
  }

  sync() {
    this.syncing = true;
    this.syncService.syncFromLastSync();
  }

  genSyncMsg(): string {
    if (this.lastSync == undefined || this.lastSync.date == undefined) {
      return "Never. Synchronize GetPocket!";
    } else {
      const lastDate = new Date(this.lastSync.date)
      return formatRelative(lastDate, new Date())
    }
  }
}
</script>