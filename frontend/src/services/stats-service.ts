import {ConfigsService} from "@/services/configs-service";
import axios from "axios";
import {format} from "date-fns";

export class StatsService {
    configsService = new ConfigsService();

    getArchivedItemsPerDay(dayStart: Date, dayEnd: Date) {
        return axios
            .post(`${this.configsService.backendUrl()}/stats/archived`, {
                start: format(dayStart, "dd-MM-yyyy"),
                end: format(dayEnd, "dd-MM-yyyy")
            });
    }
}