<template>
  <div v-if="authorized" class="card">
    <div class="card-body">
      <h5 class="card-title">Daily stats of added and read items.</h5>

      <div>
        <form class="form-inline">
          <label class="my-1 mr-2">Select period</label>

          <div class="custom-control custom-checkbox my-1 mr-sm-2">
            <Datepicker id="formDayStart" v-model="formDayStart"
                        :upper-limit="formDayEnd"
                        class="mb-2" inputFormat="dd-MM-yyyy" v-on:update:modelValue="onChangedDatePeriod"/>
          </div>

          <div class="custom-control custom-checkbox my-1 mr-sm-2">
            <Datepicker id="formDayEnd" v-model="formDayEnd"
                        :upperLimit="new Date()"
                        class="mb-2" inputFormat="dd-MM-yyyy" v-on:update:modelValue="onChangedDatePeriod"/>
          </div>

          <button class="btn btn-primary mb-2" v-on:click="onChangedDatePeriod">Apply</button>
        </form>
      </div>
      <div style="height: 25em">
        <Line v-if="loadedData" ref="itemsStats" :data="chartData" :options="chartOptions"/>
      </div>
    </div>
  </div>

</template>

<script lang="ts">
import {Options, Vue} from "vue-class-component";
import {useSessionStore} from "@/store";
import {StatsService} from "@/services/stats-service";
import {DayStatsType, IDayStat} from "@/models/stats-models";
import Datepicker from 'vue3-datepicker'
import {CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, PointElement, Title, Tooltip} from 'chart.js'
import {Line} from 'vue-chartjs'
import {subDays} from "date-fns";
import {TimePeriod} from "@/models/time-models";
import {forkJoin} from 'rxjs';

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
export default class DailyStatsChart extends Vue {
  authorized: Boolean = false

  sessionStore = useSessionStore()
  statsService = new StatsService()

  formDayStart: Date = subDays(new Date(), 7)
  formDayEnd: Date = new Date()

  chartData = {
    labels: [] as Date[],
    datasets: [
      {
        label: 'Read items in day',
        backgroundColor: '#f87979',
        data: [] as number[],
        yAxisID: 'y',
      },
      {
        label: 'Added items in day',
        backgroundColor: '#0a4687',
        data: [] as number[],
        yAxisID: 'y1',
      }
    ]
  }

  chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      y: {
        type: 'linear',
        display: true,
        suggestedMin: 0,
        suggestedMax: 100,
        steps: 5
      },
      y1: {
        type: 'linear',
        display: false,
        suggestedMin: 0,
        suggestedMax: 100,
        steps: 5
      },
    }
  }

  loadedData: boolean = false
  componentSelf = this;

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized
  }

  mounted() {
    this.onChangedDatePeriod();

    this.isAuthorized()
    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized
    });
  }

  clearChart() {
    this.chartData.labels.slice(0)
    this.chartData.datasets[0].data.slice(0)
    this.chartData.datasets[1].data.slice(0)
  }

  onChangedDatePeriod() {
    this.clearChart()

    const self = this.componentSelf;
    self.loadedData = false;

    const timePeriod = new TimePeriod(self.formDayStart, self.formDayEnd);

    forkJoin([
      this.statsService.getStatsPerDay(timePeriod, DayStatsType.ARCHIVED),
      this.statsService.getStatsPerDay(timePeriod, DayStatsType.TODO)
    ]).subscribe(results => {
      let archivedDayStats = results[0].data.stats;
      let addedDayStats = results[1].data.stats;

      //labels
      let newLabels = new Set<Date>()
      addedDayStats.reverse().forEach((dayData: IDayStat) => {
        newLabels.add(dayData.day)
      })

      archivedDayStats.reverse().forEach((dayData: IDayStat) => {
        newLabels.add(dayData.day)
      })

      //merge data
      let todoDataset = [] as number[]
      let archDataset = [] as number[]

      const todoByDate = new Map(addedDayStats.map((i: IDayStat) => [i.day, i.number]));
      const archByDate = new Map(archivedDayStats.map((i: IDayStat) => [i.day, i.number]));

      let maxY = 0
      let sortedLables = Array.from(newLabels).sort();
      sortedLables.forEach(dayDate => {
        if (todoByDate.has(dayDate)) {
          const value = todoByDate.get(dayDate) as number;

          if (value > maxY)
            maxY = value

          todoDataset.push(value)
        } else {
          todoDataset.push(0)
        }

        if (archByDate.has(dayDate)) {
          const value = archByDate.get(dayDate) as number;

          if (value > maxY)
            maxY = value

          archDataset.push(value)
        } else {
          archDataset.push(0)
        }
      });

      self.chartOptions.scales.y.suggestedMax = maxY;
      self.chartOptions.scales.y1.suggestedMax = maxY;

      self.chartData.labels = sortedLables
      self.chartData.datasets[0].data = archDataset
      self.chartData.datasets[1].data = todoDataset

      self.loadedData = true
    })
  }
}

</script>