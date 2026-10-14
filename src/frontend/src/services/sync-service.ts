import {useSyncStore} from "@/store";
import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {SyncStatus} from "@/models/sync-models";

export class SyncService {
    syncStore = useSyncStore();
    configsService = new ConfigsService();

    async isAvailable(): Promise<boolean> {
        const response = await axios.get(`${this.configsService.backendUrl()}/sync/availability`);
        return response.data.data.available === true;
    }

    async updateLastSyncStatus(): Promise<SyncStatus> {
        const response = await axios.get(`${this.configsService.backendUrl()}/sync/last/READER`);
        return this.storeStatus(response.data.data);
    }

    async syncFromLastSync(): Promise<SyncStatus> {
        const response = await axios.post(`${this.configsService.backendUrl()}/sync/last/READER`, undefined,
            {headers: {"X-PocketStats-Sync": "1"}});
        return this.storeStatus(response.data.data);
    }

    private storeStatus(data: { date: string | null; records: number }): SyncStatus {
        const status = new SyncStatus(data.date ? new Date(data.date) : undefined, data.records);
        this.syncStore.setSyncStatus("READER", status);
        return status;
    }
}
