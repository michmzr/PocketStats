import {useSyncStore} from "@/store";
import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {SyncStatus} from "@/models/sync-models";

export class SyncService {
    syncStore = useSyncStore();
    configsService = new ConfigsService();

    updateLastSyncStatus() {
        axios
            .get(`${this.configsService.backendUrl()}/sync/last`)
            .then((response) => {
                const data = response.data.data;
                const syncStatus = new SyncStatus(data.date, data.records);

                this.syncStore.setSyncStatus(syncStatus);
            });
    }

    syncFromLastSync() {
        axios
            .post(`${this.configsService.backendUrl()}/sync/last`)
            .then((response) => {
                const data = response.data.data;

                const date = new Date(Date.parse(data.date));
                const syncStatus = new SyncStatus(date, data.records);

                this.syncStore.setSyncStatus(syncStatus);
            });
    }
}