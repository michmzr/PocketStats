<template>
  <div v-if="authorized" class="card">
    <div v-if="loadedData" class="card-body">
      <h5 class="card-title">Top {{ limitTags }} the most popular tags.</h5>
      <span v-for="item in topTags" v-bind:key="item.name" class="badge ">
              <b-badge variant="dark">{{ item.name }} <b-badge variant="light">{{ item.count }}</b-badge></b-badge>
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {useSessionStore, useSyncStore} from "@/store";
import {StatsService} from "@/services/stats-service";
import {ITopTag, ITopTags} from "@/models/stats-models";

export default class TopTags extends Vue {
  limitTags: number = 30

  authorized: Boolean = false
  loadedData: boolean = false

  sessionStore = useSessionStore()
  statsService = new StatsService()
  syncStore = useSyncStore()

  topTags: ITopTag[] = [] as ITopTag[]

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized;
  }

  mounted() {
    this.loadedData = false;

    this.isAuthorized();

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized;
    });

    this.syncStore.$subscribe(() => {
      console.debug(`Got sync store change - reloading component data`)
      this.loadTopTags()
    })
  }

  loadTopTags() {
    let self = this

    this.statsService.getTopTags(this.limitTags).then((responseData: ITopTags) => {
      self.topTags = responseData.tags
      self.loadedData = true
    });
  }
}


</script>