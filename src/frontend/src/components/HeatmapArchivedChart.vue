<template>
  <div v-if="authorized && loadedData" class="card">
    <div class="card-body">
      <h5 class="card-title">{{ header }}</h5>

      <div style="width: 100%">
        <apexchart :options="options" :series="series" type="heatmap"></apexchart>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Vue} from "vue-class-component";
import {Prop} from 'vue-property-decorator';
import {useSessionStore, useSyncStore} from "@/store";
import {StatsService} from "@/services/stats-service";
import {HeatmapType, IActivityHeatmapItem, IActivityHeatmapStats,} from "@/models/stats-models";

export default class HeatmapArchivedChart extends Vue {

  @Prop({type: String, required: true})
  header!: string;

  @Prop({type: String, default: HeatmapType.ARCHIVED})
  type!: string;

  authorized: Boolean = false
  loadedData: boolean = true

  sessionStore = useSessionStore()
  syncStore = useSyncStore()
  statsService = new StatsService()

  items: IActivityHeatmapItem[] = []

  options = {
    chart: {
      id: 'heatmap',
      width: '100%'
    },
    plotOptions: {
      heatmap: {
        distributed: true,
        colorScale: {
          ranges: [
            {from: 0, to: 30, color: '#00A100', name: 'very low'},
            {from: 31, to: 60, color: '#44B100', name: 'low'},
            {from: 61, to: 90, color: '#88C100', name: 'low-medium'},
            {from: 91, to: 120, color: '#B4D200', name: 'medium'},
            {from: 121, to: 150, color: '#E0E300', name: 'medium-high'},
            {from: 151, to: 180, color: '#FFB200', name: 'high'},
            {from: 181, to: 210, color: '#FF8000', name: 'very high'},
            {from: 211, to: 240, color: '#FF5C00', name: 'extremely high'},
            {from: 241, to: 270, color: '#FF3200', name: 'critical'},
            {from: 271, to: 300, color: '#FF0000', name: 'max'}
          ]
        }
      }
    }
  }
  series: Object[] = []

  isAuthorized() {
    this.authorized = this.sessionStore.isAuthorized
  }

  mounted() {
    this.isAuthorized()

    this.sessionStore.$subscribe((mutation, state) => {
      this.authorized = state.authorized
    });

    this.syncStore.$subscribe(() => {
      console.debug(`Got sync store change - reloading component data`)

      this.refreshHeatmap()
    })
  }

  refreshHeatmap() {
    let self = this

    self.items = []
    self.series = []
    self.loadedData = false

    const statType = HeatmapType.ARCHIVED === this.type ? HeatmapType.ARCHIVED : HeatmapType.TODO

    this.statsService.getHeatmapOfArchived(statType)
        .then((responseData: IActivityHeatmapStats) => {
          self.items = responseData.items

          self.prepareChatSeries()

          self.loadedData = true
        });
  }

  prepareChatSeries() {
    type WeekdayData = { name: string, data: Array<{ x: string, y: number }> };
    const weekdays: WeekdayData[] = [
      {name: "Monday", data: []}, //0
      {name: "Tuesday", data: []},  // 1
      {name: "Wednesday", data: []},
      {name: "Thursday", data: []},
      {name: "Friday", data: []},
      {name: "Saturday", data: []},
      {name: "Sunday", data: []}, //6
    ]

    const diffUserZoneToUTC = this.utcToUserTimeZoneDifferenceInHours()

    // Iterate through the input data and populate the weekdays object
    this.items.forEach((item: IActivityHeatmapItem) => {
      let hour = (item.hour + diffUserZoneToUTC) % 24;
      weekdays[item.weekday - 1]!.data.push({x: `${hour}`, y: item.count});
    })

    // Sort the data arrays for each weekday by x in ascending order
    weekdays.forEach((weekday) => {
      weekday.data = weekday.data.sort((a, b) => {
        const aX = parseInt(a.x)
        const bX = parseInt(b.x)

        return aX - bX
      });
    });

    this.series = Object.values(weekdays);
  }

  utcToUserTimeZoneDifferenceInHours() {
    Intl.DateTimeFormat().resolvedOptions().timeZone
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const offsetMinutes = new Date().getTimezoneOffset();
    const diff = -offsetMinutes / 60;

    console.log(`Timezone: ${timeZone}, Difference to UTC in hours: ${diff}`);

    return diff
  }
}

</script>
