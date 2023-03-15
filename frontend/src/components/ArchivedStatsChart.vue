<template>
  <div class="card" v-if="authorized && loadedData">
    <div class="card-body">
      <div>
        <label for="example-datepicker">Choose a days</label>
        <Datepicker id="formDayStart" v-model="formDayStart"
                    v-on:update:modelValue="onChangedDatePeriod"
                    :upper-limit="formDayEnd" class="mb-2" inputFormat="dd-MM-yyyy"/>
        <Datepicker id="formDayEnd" v-model="formDayEnd"
                    v-on:update:modelValue="onChangedDatePeriod"
                    :upperLimit="new Date()" class="mb-2" inputFormat="dd-MM-yyyy"/>
      </div>
      <div>
        <Line v-if="loadedData" :data="chartData" :options="chartOptions"/>
      </div>
    </div>
  </div>

</template>

<script lang="ts">
import {Options, Vue} from "vue-class-component";
import {useSessionStore} from "@/store";
import {StatsService} from "@/services/stats-service";
import {IDayStats} from "@/models/stats-models";
import Datepicker from 'vue3-datepicker'
import {CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, PointElement, Title, Tooltip} from 'chart.js'
import {Line} from 'vue-chartjs'
import {subDays} from "date-fns";

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
)

@Options({
  components: {
    Line: Line,
    Datepicker: Datepicker
  },
})
export default class ArchivedStatsChart extends Vue {
  loadedData: boolean = false
  authorized: Boolean = false
  sessionStore = useSessionStore()
  statsService = new StatsService()

  formDayStart: Date = subDays(new Date(), 7)
  formDayEnd: Date = new Date()

  daysStats: IDayStats | undefined

  chartData = {
    labels: [] as Date[],
    datasets: [
      {
        label: 'Archived items',
        backgroundColor: '#f87979',
        data: [] as number[]
      }
    ]
  }

  chartOptions = {
    responsive: true,
    maintainAspectRatio: false
  }


  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized
  }

  mounted() {
    this.loadedData = false
    this.daysStats = undefined
    this.onChangedDatePeriod();

    this.isAuthorized()
    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized
    });

  }

  clearChart() {
    this.chartData.labels = []
    this.chartData.datasets[0].data = []
  }

  onChangedDatePeriod() {
    this.loadedData = false;

    this.statsService.getArchivedItemsPerDay(this.formDayStart, this.formDayEnd)
        .then((response) => {
          this.onLoadedStatsData(response.data.data as IDayStats)
        });

    return;
  }

  onLoadedStatsData(daysStats: IDayStats) {
    console.info("Data loaded")

    this.loadedData = true
    this.daysStats = daysStats
    this.clearChart()

    const self = this;
    daysStats.stats.reverse().forEach(dayStats => {
      self.chartData.labels.push(dayStats.day)
      self.chartData.datasets[0].data.push(dayStats.number)
    })
  }
}

</script>