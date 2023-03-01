<template>
  <div>
    <div v-if="!authorized">
      <a v-if="loginUrl" v-bind:href="loginUrl" target="_blank">
        Log to pocket
      </a>
    </div>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import {useSessionStore} from '@/store';
import {AuthorizationService} from "@/services/authorization-service";

@Options({
  props: {
  }
})
export default class HomeAuth extends Vue {
  authorized: Boolean = false;
  loginUrl?: String;
  sessionStore = useSessionStore();

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized;
    this.onAuthorizationStatusChanged();
  }

  onAuthorizationStatusChanged() {
    if(!this.authorized && this.loginUrl == undefined) {
      let authService = new AuthorizationService();

      let self = this;
       authService.getLoginUrl().then((response) => {
         self.loginUrl = response.data.data.link;
       })
    }
  }

  mounted(){
    this.authorized = false;
    this.loginUrl = undefined;

    this.isAuthorized();
    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized;
      this.onAuthorizationStatusChanged();
    })
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
