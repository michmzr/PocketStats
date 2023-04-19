<template>
  <div class="row mt-2">
    <div class="col">
      <TopTools></TopTools>
    </div>
  </div>

  <div class="row mt-2">
    <div class="col">
      <DailyStatsChart></DailyStatsChart>
    </div>
  </div>

  <div class="row mt-2">
    <div class="col-6">
      <TopTags></TopTags>
    </div>
    <div class="col-6">
      <StatsPerPeriod></StatsPerPeriod>
    </div>
  </div>

  <div class="row mt-2">
    <div class="col">
      <LangStats></LangStats>
    </div>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import DailyStatsChart from "@/components/DailyStatsChart.vue";
import TopTools from "@/components/SyncPanel.vue";
import TopTags from "@/components/TopTags.vue";
import LangStats from "@/components/LangStats.vue";
import StatsPerPeriod from "@/components/StatsPerPeriod.vue";

@Options({
  components: {
    LangStats,
    TopTags,
    DailyStatsChart,
    TopTools,
    StatsPerPeriod
  },
})
export default class HomeView extends Vue {
}
</script>
