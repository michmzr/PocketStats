<template>
  <div v-if="authorized" class="card">
    <div v-if="loadedData" class="card-body">
      <h5 class="card-title">Stats per time period</h5>

      <table class="table table-hover">
        <thead>
        <tr>
          <th>📅 Period</th>
          <th>🗓️ Date Range</th>
          <th>🆕 Added</th>
          <th>✅ Read</th>
        </tr>
        </thead>
        <tbody>

        <tr v-for="periodStats in  itemsStats" v-bind:key="periodStats.nameShort">
          <td>{{ periodStats.nameDesc }}</td>
          <td>
               <span
                   v-if="periodStats.period">{{ displayPeriod(periodStats.period) }}</span>
          </td>
          <td>{{ periodStats.stats.added }}</td>
          <td>{{ periodStats.stats.read }}</td>
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
import {ItemsStatsAggregated, ItemsStatsPerPeriod, TimePeriod} from "@/models/stats-models";
import {format, parseISO} from "date-fns";

export default class StatsPerPeriod extends Vue {
  authorized: Boolean = false
  loadedData: boolean = false

  sessionStore = useSessionStore()
  statsService = new StatsService()

  itemsStats: ItemsStatsPerPeriod[] = [];

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized
  }

  mounted() {
    this.loadedData = false

    this.isAuthorized()

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized
    })

    this.loadStats()
  }

  displayPeriod(period: TimePeriod): String {
    const begin = new Date(Date.parse(period.begin));
    const end = new Date(Date.parse(period.end));

    const pretty = format(parseISO(period.begin.toString()), "dd-MM-yyyy") + " - " + format(parseISO(period.end.toString()), "dd-MM-yyyy");
    return pretty
  }

  loadStats() {
    let self = this

    this.statsService.getPeriodStats()
        .then((responseData: ItemsStatsAggregated) => {
          self.itemsStats = responseData.itemsStats
          self.loadedData = true
        });
  }
}
</script>