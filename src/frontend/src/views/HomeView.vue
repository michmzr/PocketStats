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
    <div class="col-md-6 col-12 col-lg-4 block">
      <TopTags></TopTags>
    </div>
    <div class="col-md-6 col-12 col-lg-8 block">
      <StatsPerPeriod></StatsPerPeriod>
    </div>
  </div>

  <div class="row mt-2">
    <div class="col-md-6 col-12 col-lg-2 block">
      <LangStats></LangStats>
    </div>
    <div class="col-md-6 col-12 col-lg-10 block">
      <HeatmapArchivedChart :header="'Archivity Activity Heatmap'" :type="'ARCHIVED'"/>
    </div>

    <div class="col-md-6 col-12 col-lg-10 block">
      <HeatmapArchivedChart :header="'Adding Activity Heatmap'" :type="'TODO'"/>
    </div>
  </div>

  <div class="row mt-2">

  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import DailyStatsChart from "@/components/DailyStatsChart.vue";
import TopTools from "@/components/SyncPanel.vue";
import TopTags from "@/components/TopTags.vue";
import LangStats from "@/components/LangStats.vue";
import StatsPerPeriod from "@/components/StatsPerPeriod.vue";
import HeatmapArchivedChart from "@/components/HeatmapArchivedChart.vue";
import {HeatmapType} from "@/models/stats-models";

@Options({
  computed: {
    HeatmapType() {
      return HeatmapType
    }
  },
  components: {
    LangStats,
    TopTags,
    DailyStatsChart,
    TopTools,
    StatsPerPeriod,
    HeatmapArchivedChart
  },
})
export default class HomeView extends Vue {
}
</script>
