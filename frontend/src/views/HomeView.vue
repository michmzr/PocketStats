<template>
  <div class="row mt-1">
    <div class="col">
      <TopTools></TopTools>
    </div>
  </div>

  <div class="row mt-1">
    <div class="col">
      <DailyStatsChart></DailyStatsChart>
    </div>
  </div>

  <div class="row mt-1">
    <div class="col">
      <TopTags></TopTags>
    </div>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import DailyStatsChart from "@/components/DailyStatsChart.vue";
import TopTools from "@/components/SyncPanel.vue";
import TopTags from "@/components/TopTags.vue";

@Options({
  components: {
    TopTags,
    DailyStatsChart,
    TopTools
  },
})
export default class HomeView extends Vue {
}
</script>
