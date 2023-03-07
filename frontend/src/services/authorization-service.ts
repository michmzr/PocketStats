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
            .get(`${this.configsService.backendUrl()}/pocket/auth/login`);
    }

    waitForAuthorization(): Promise<boolean> {
        const RETRY_DELAY_MS = 1500; // delay between retry attempts in milliseconds
        const MAX_RETRIES = 15; // maximum number of retries

        async function retryAuthorized(self: AuthorizationService): Promise<boolean> {
            let retries = 0;
            while (retries <= MAX_RETRIES) {
                try {
                    const response = await self.getAuthorizationStatusFromBackend();

                    if (response.data.data.status === true) {
                        console.log('Authorized!');
                        return true; // successful response, exit retry loop
                    } else {
                        console.log('Not authorized yet...');
                    }
                } catch (error: any) {
                    console.log(`Error: ${error.message}`);
                }

                retries++;
                if (retries >= MAX_RETRIES) {
                    console.log(`Maximum retries reached (${MAX_RETRIES}), giving up...`);
                    return false;
                }

                console.log(`Retrying in ${RETRY_DELAY_MS} ms...`);
                await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
            }

            return false;
        }

        //todo update pina state
        return retryAuthorized(this);
    }
}