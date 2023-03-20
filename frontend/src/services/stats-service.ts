import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {format} from "date-fns";
import {TimePeriod} from "@/models/time-models";
import {DayStatsType} from "@/models/stats-models";

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
}