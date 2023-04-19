import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {format} from "date-fns";
import {TimePeriod} from "@/models/time-models";
import {DayStatsType, ILangStats, ItemsStatsAggregated, ITopTags,} from "@/models/stats-models";

export class StatsService {
    configsService = new ConfigsService();

    getStatsPerDay(period: TimePeriod, type: DayStatsType) {
        return axios
            .post(`${this.configsService.backendUrl()}/stats/byDay`, {
                start: format(period.start!, "dd-MM-yyyy"),
                end: format(period.end!, "dd-MM-yyyy"),
                type: type
            }).then((response) => {
                return response.data
            });
    }

    getTopTags(count: number) {
        return axios
            .get(`${this.configsService.backendUrl()}/stats/topTags?count=${count}`)
            .then((response) => {
                return response.data.data as ITopTags
            });
    }

    getLangStats() {
        return axios
            .get(`${this.configsService.backendUrl()}/stats/langs`)
            .then((response) => {
                return response.data.data as ILangStats
            });
    }

    getPeriodStats() {
        return axios
            .get(`${this.configsService.backendUrl()}/stats/byPeriods`)
            .then((response) => {
                return response.data.data as ItemsStatsAggregated
            });
    }
}