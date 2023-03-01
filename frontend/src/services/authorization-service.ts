import axios from 'axios'
import {useSessionStore} from "@/store";
import {ConfigsService} from "@/services/configs-service";

export class AuthorizationService {

    sessionStore = useSessionStore();
    configsService = new ConfigsService();

    getAuthorizationStatusFromBackend() {
        return axios
            .get(`${this.configsService.backendUrl()}/pocket/auth/authorized`);
    }

    updateAuthorizationState() {
        this.getAuthorizationStatusFromBackend().then((response) => {
            this.sessionStore.setAuthorizedState(response.data.data.status)
        })
    }

    getLoginUrl() {
        return axios
            .get(`${this.configsService.backendUrl()}/pocket/auth/login_url`);
    }
}