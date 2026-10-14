<template>
  <div id="SyncPanel-READER" class="card">
    <div class="card-body">
      <span v-if="!loadedData">Checking Readwise synchronization...</span>
      <template v-else>
        <strong>Last sync Readwise:</strong> <span>{{ lastSyncMsg }}</span>
        <b-button v-if="available" variant="link" :disabled="syncing"
                  size="sm" v-on:click="sync">
          <span v-if="!syncing">Sync Readwise</span>
          <span v-else>
            <b-spinner small type="grow"></b-spinner>
            Loading
          </span>
        </b-button>
        <p v-if="!available && !error" class="mb-0">
          {{ privateDashboard ? 'Synchronization is disabled for this private dashboard.' : 'Readwise synchronization is not configured.' }}
        </p>
      </template>
      <p v-if="error" role="alert" class="text-danger mb-0">{{ error }}</p>
    </div>
  </div>
</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {SyncStatus} from "@/models/sync-models";
import {SyncService} from "@/services/sync-service";
import {formatRelative} from "date-fns";

export default class SyncPanel extends Vue {
  privateDashboard = window.POCKETSTATS_CONFIG?.privateDashboard === true;
  available = false;
  syncing = false;
  loadedData = false;
  error: string | null = null;
  lastSync: SyncStatus | null = null;
  syncService = new SyncService();

  async mounted() {
    try {
      this.available = await this.syncService.isAvailable();
      if (!this.privateDashboard) {
        this.lastSync = await this.syncService.updateLastSyncStatus();
      }
    } catch {
      this.error = "Could not load Readwise synchronization status. Reload the page to try again.";
    } finally {
      this.loadedData = true;
    }
  }

  async sync() {
    if (!this.available || this.syncing) return;
    this.syncing = true;
    this.error = null;
    try {
      this.lastSync = await this.syncService.syncFromLastSync();
    } catch {
      this.error = "Readwise synchronization failed. Please try again.";
    } finally {
      this.syncing = false;
    }
  }

  get lastSyncMsg(): string {
    if (!this.lastSync || !this.lastSync.date) return "Never.";
    return formatRelative(this.lastSync.date, new Date())
        + ", synchronized items: " + this.lastSync.records;
  }
}
</script>
