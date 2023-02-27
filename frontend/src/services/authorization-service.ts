import axios from 'axios'
import {useSessionStore} from "@/store";

export class AuthorizationService {
    sessionStore = useSessionStore();

    getAuthorizationStatus() {
        return axios
            .get('http://localhost:8080/pocket/auth/authorized');
    }

    updateAuthorizationState() {
        this.getAuthorizationStatus().then((response) => {
            console.log(response.data.data.status);
            this.sessionStore.setAuthorizedState(response.data.data.status)
        })
    }
}