<template>
  <form class="form-inline my-2 my-lg-0" >
    <div v-if="!authorized && waitingForLogin === false" :key="authorized">
      <a v-if="showLoginButton" v-bind:href="loginUrl" target="_blank" v-on:click="loginStarted">
        <button class="btn btn-outline-primary my-2 my-sm-0" type="submit">Log to pocket</button>
      </a>
    </div>

    <button class="btn btn-light btn-outline-primary" v-if="waitingForLogin" disabled>
      <font-awesome-icon icon="spinner" spin/>
      Waiting...
    </button>

    <label v-if="authorized">
      <font-awesome-icon icon="user"/>
    </label>
  </form>

</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {useSessionStore} from "@/store";
import {AuthorizationService} from "@/services/authorization-service";

export default class TopAuth extends Vue {
  waitingForLogin: Boolean = false
  authorized: Boolean = false

  showLoginButton:Boolean = false
  loginUrl: String = ""

  sessionStore = useSessionStore()

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized;
    this.onAuthorizationStatusChanged();
  }

  onAuthorizationStatusChanged() {
    if (!this.showLoginButton) {
      let authService = new AuthorizationService()

      let self = this
      authService.getLoginUrl().then((response) => {
        self.loginUrl = response.data.data.link
        self.showLoginButton = true
      });
    }
  }

  loginStarted() {
    this.waitingForLogin = true
    let authService = new AuthorizationService()

    let self = this
    authService.waitForAuthorization().then((value) => {
      self.waitingForLogin = false
      self.authorized = value
    });
  }

  mounted() {
    this.authorized = false
    this.waitingForLogin = false

    this.isAuthorized()

    let self = this
    this.sessionStore.$subscribe((mutation, state) => {
      console.info("store auth status")
      self.authorized = state.authorized
      self.onAuthorizationStatusChanged()
    })
  }
}
</script>
