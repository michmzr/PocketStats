import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {format} from "date-fns";
import {TimePeriod} from "@/models/time-models";

export class StatsService {
    configsService = new ConfigsService();

    getArchivedItemsPerDay(period: TimePeriod) {
        return axios
            .post(`${this.configsService.backendUrl()}/stats/archived`, {
                start: format(period.start!, "dd-MM-yyyy"),
                end: format(period.end!, "dd-MM-yyyy")
            });
    }
}