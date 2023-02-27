<template>
  <div class="hello">
    <p v-if="authorized">Dostęp do ApiPocketa</p>
    <p v-else>Zaloguj się do Pocket</p>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import {AuthorizationService} from "@/services/authorization-service";
import {useSessionStore} from '@/store';

@Options({
  props: {
  }
})
export default class TopAuth extends Vue {
   authorized: Boolean = false;
  sessionStore = useSessionStore();

  isAuthorized() {
    this.authorized = false;

    this.authorized = this.sessionStore.isAuthorized;

    let as = new AuthorizationService();
    as.updateAuthorizationState();
  }

  mounted(){
    this.authorized = false;
    this.isAuthorized();

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized;
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
