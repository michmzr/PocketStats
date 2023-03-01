
export class ConfigsService {
    backendUrl(): String {
        return process.env.VUE_APP_BACKEND_URL;
    }
}