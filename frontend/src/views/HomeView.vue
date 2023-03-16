<template>
  <div class="row mt-1">
    <div class="col">
      <TopTools></TopTools>
    </div>
  </div>

  <div class="row mt-1">
    <div class="col">
      <ArchivedStatsChart></ArchivedStatsChart>
    </div>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import ArchivedStatsChart from "@/components/ArchivedStatsChart.vue";
import TopTools from "@/components/SyncPanel.vue";

@Options({
  components: {
    ArchivedStatsChart,
    TopTools
  },
})
export default class HomeView extends Vue {
}
</script>
