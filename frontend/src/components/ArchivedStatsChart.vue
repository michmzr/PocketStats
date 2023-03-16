<template>
  <div class="card" v-if="authorized && loadedData">
    <div class="card-body">
      <div>
        <form class="form-inline">
          <label class="my-1 mr-2">Choose days</label>

          <div class="custom-control custom-checkbox my-1 mr-sm-2">
            <Datepicker id="formDayStart" v-model="formDayStart"
                        v-on:update:modelValue="onChangedDatePeriod"
                        :upper-limit="formDayEnd" class="mb-2" inputFormat="dd-MM-yyyy"/>
          </div>

          <div class="custom-control custom-checkbox my-1 mr-sm-2">
            <Datepicker id="formDayEnd" v-model="formDayEnd"
                        v-on:update:modelValue="onChangedDatePeriod"
                        :upperLimit="new Date()" class="mb-2" inputFormat="dd-MM-yyyy"/>
          </div>

          <button v-on:click="onChangedDatePeriod" class="btn btn-primary mb-2">Apply</button>
        </form>
      </div>
      <div style="height: 25em">
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
import {TimePeriod} from "@/models/time-models";

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
        label: 'Read items in day',
        backgroundColor: '#f87979',
        data: [] as number[]
      }
    ]
  }

  chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      y: {
        suggestedMin: 0,
        suggestedMax: 100,
        steps: 5
      }
    }
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

    this.statsService.getArchivedItemsPerDay(new TimePeriod(this.formDayStart, this.formDayEnd))
        .then((response) => {
          this.onLoadedStatsData(response.data.data as IDayStats)
        });

    return;
  }

  onLoadedStatsData(daysStats: IDayStats) {
    this.loadedData = true
    this.daysStats = daysStats
    this.clearChart()

    const self = this;
    let maxY = 0;
    daysStats.stats.reverse().forEach(dayStats => {
      self.chartData.labels.push(dayStats.day)
      self.chartData.datasets[0].data.push(dayStats.number)

      if (dayStats.number > maxY)
        maxY = dayStats.number
    })

    this.chartOptions.scales.y.suggestedMax = maxY;
  }
}

</script>