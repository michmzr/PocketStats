declare global {
    interface Window { POCKETSTATS_CONFIG?: { backendUrl: string }; }
}

export class ConfigsService {
    backendUrl(): string {
        return window.POCKETSTATS_CONFIG?.backendUrl ?? "";
    }
}
