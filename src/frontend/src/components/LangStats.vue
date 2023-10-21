<template>
  <div v-if="authorized" class="card">
    <div class="card-body">
      <h5 class="card-title">Languages popularity</h5>

      <div style="height: 30em; width: auto">
        <Pie v-if="loadedData" :data="chartData" :options="chartOptions"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from "vue-class-component";
import {useSessionStore, useSyncStore} from "@/store";
import {Pie} from "vue-chartjs";
import {ILangStats} from "@/models/stats-models";
import {StatsService} from "@/services/stats-service";
import {ArcElement, Chart as ChartJS, Legend, Tooltip} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend)

@Options({
  components: {Pie},
})
export default class LangStats extends Vue {
  authorized: Boolean = false;
  sessionStore = useSessionStore();
  syncStore = useSyncStore()

  loadedData: boolean = false;

  componentSelf = this;

  chartData = {
    labels: [] as string[],
    datasets: [{
      data: [] as number[],
      backgroundColor: [] as string[]
    }]
  };

  chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: 0
  }

  statsService: StatsService = new StatsService()

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
      this.loadData()
    })
  }

  loadData() {
    this.statsService.getLangStats().then((responseData: ILangStats) => {
      let langStats = responseData
      const labels: string[] = [];
      const values: number[] = [];
      let total: number = 0;

      for (let [key, value] of Object.entries(langStats.langCount)) {
        labels.push(key);
        values.push(value);
        total += value;
      }

      const chartLabels: string[] = [];
      const chartDatasets: number[] = [];

      //get 2 most frequent languages from datasets and related labels
      let mostFrequentLangs = values.slice().sort((a, b) => b - a).slice(0, 2)
      //get names of the most popular languages from labels and values
      let mostFrequentLangsNames = labels.filter((label, index) => {
        return mostFrequentLangs.includes(values[index])
      })

      for (let [key, value] of Object.entries(langStats.langCount)) {
        if (!mostFrequentLangsNames.includes(key)) {
          continue
        }

        chartLabels.push(`${key} (${value})`);
        chartDatasets.push(value);
      }

      chartLabels.push(`other (${total - chartDatasets.reduce((a, b) => a + b, 0)})`)
      chartDatasets.push(total - chartDatasets.reduce((a, b) => a + b, 0))

      this.chartData.labels = chartLabels;
      this.chartData.datasets[0].data = chartDatasets

      let colors: string[] = []
      for (let i = 0; i < chartDatasets.length; i++) {
        colors.push(this.getRandomColor())
      }
      this.chartData.datasets[0].backgroundColor = colors;

      this.componentSelf.loadedData = true
    })
  }

  getRandomColor() {
    const letters = '0123456789ABCDEF'.split('');
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }
}


</script>