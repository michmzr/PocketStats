import {useSyncStore} from "@/store";
import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {SyncStatus} from "@/models/sync-models";

export class SyncService {
    syncStore = useSyncStore();
    configsService = new ConfigsService();

    updateLastSyncStatus(source: string) {
        console.log('updateLastSyncStatus', source);
        axios
            .get(`${this.configsService.backendUrl()}/sync/last/${source}`)
            .then((response) => {
                const data = response.data.data;
                const syncStatus = new SyncStatus(data.date, data.records);

                this.syncStore.setSyncStatus(source, syncStatus);
            });
    }

    syncFromLastSync(source: string) {
        console.log('syncFromLastSync', source);

        axios
            .post(`${this.configsService.backendUrl()}/sync/last/${source}`)
            .then((response) => {
                const data = response.data.data;

                const date = new Date(Date.parse(data.date));
                const syncStatus = new SyncStatus(date, data.records);

                this.syncStore.setSyncStatus(source, syncStatus);
            });
    }
}