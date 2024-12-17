<template>
  <div v-if="authorized && loadedData" id="SyncPanel-{{source}}" class="card">
    <div class="card-body">
      <strong>Last sync {{ source }}:</strong> {{ lastSyncMsg }}

      <b-button variant="link" :disabled="syncing"
                size="sm" v-on:click="sync">
        <span v-if="!syncing">
          <font-awesome-icon icon="spinner"/>
          Sync {{source}}
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
import {SyncState, useSessionStore, useSyncStore} from "@/store";
import {SyncStatus} from "@/models/sync-models";
import {SyncService} from "@/services/sync-service";
import {formatRelative} from "date-fns";
import {Prop} from "vue-property-decorator";

export default class SyncPanel extends Vue {

  @Prop({type: String, required: true})
  source!: string;

  authorized: Boolean = false;
  sessionStore = useSessionStore();
  syncStore = useSyncStore();
  syncing: boolean = false;
  loadedData: boolean = false;

  lastSync: SyncStatus | undefined = undefined;
  lastSyncMsg: string | undefined;
  syncService: SyncService = new SyncService();

  selfComponent = this;

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized;
  }

  mounted() {
    this.selfComponent = this;

    this.isAuthorized();

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized;
    });

    this.syncStore.$subscribe((mutation, state: SyncState) => {
      console.debug("Got change in sync store -> SyncStore mutation: " + mutation.type);
      this.syncing = false;
      this.lastSync = state.readers.get(this.source);
      this.loadedData = true;

      this.lastSyncMsg = this.genSyncMsg();
    });
  }

  sync() {
    console.log("Syncing items from " + this.source);
    this.syncing = true;
    this.syncService.syncFromLastSync(this.source);
  }

  genSyncMsg(): string {
    if (this.lastSync == undefined || this.lastSync.date == undefined) {
      return "Never.";
    } else {
      const lastDate = new Date(this.lastSync.date)
      return formatRelative(lastDate, new Date()) + ", synchronized items: " + this.lastSync.records;
    }
  }
}
</script>