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
import {useSessionStore} from "@/store";
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

    this.loadData();
  }

  loadData() {
    this.statsService.getLangStats().then((responseData: ILangStats) => {
      let langStats = responseData
      const labels: string[] = [];
      const values: number[] = [];

      for (let [key, value] of Object.entries(langStats.langCount)) {
        labels.push(`${key} (${value})`);
        values.push(value);
      }

      this.chartData.labels = labels;
      this.chartData.datasets[0].data = values

      let colors: string[] = []
      for (let i = 0; i < values.length; i++) {
        colors.push(this.getRandomColor())
      }
      this.chartData.datasets[0].backgroundColor = colors;

      this.componentSelf.loadedData = true
    })
  }

  getRandomColor() {
    var letters = '0123456789ABCDEF'.split('');
    var color = '#';
    for (var i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

}


</script>