<template>
  <div v-if="authorized && loadedData" :id="'SyncPanel-' + source" class="card">
    <div class="card-body">
      <strong>Last sync {{ source }}:</strong><span v-if="lastSync">{{ lastSyncMsg }}</span>

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
import {SubscriptionCallbackMutation} from "pinia";

export default class SyncPanel extends Vue {

  @Prop({type: String, required: true})
  source!: string;

  authorized: boolean = false;
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

    this.syncStore.$subscribe((mutation, state: SyncState) => this.onSynced(mutation, state));
  }

  sync() {
    console.log("Syncing items from " + this.source);
    this.syncing = true;
    this.syncService.syncFromLastSync(this.source);
  }

  onSynced(mutation: SubscriptionCallbackMutation<SyncState>, state: SyncState) {
    console.debug(`[${this.source}] Got change in sync store`);

    this.syncing = false;
    this.lastSyncMsg = undefined;

    this.lastSync = state.readers.get(this.source);
    this.lastSyncMsg = this.genSyncMsg();
    this.loadedData = true;
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