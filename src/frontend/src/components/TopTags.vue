<template>
  <div class="card" v-if="authorized">
    <div class="card-body" v-if="loadedData">
      <h5 class="card-title">Top {{ limitTags }} the most popular tags.</h5>
      <table class="table">
        <thead>
        <tr>
          <th scope="col">Tag name</th>
          <th scope="col">Occurs</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in topTags" v-bind:key="item.name">
          <td>{{ item.name }}</td>
          <td>{{ item.count }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {useSessionStore} from "@/store";
import {StatsService} from "@/services/stats-service";
import {ITopTag, ITopTags} from "@/models/stats-models";

export default class TopTags extends Vue {
  limitTags: number = 10

  authorized: Boolean = false
  loadedData: boolean = false

  sessionStore = useSessionStore()
  statsService = new StatsService()

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

    this.loadTopTags();
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