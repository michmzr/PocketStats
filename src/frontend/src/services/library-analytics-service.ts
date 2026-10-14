import axios from 'axios';
import {ConfigsService} from '@/services/configs-service';
import {AnalyticsSource, ComparisonDays, LibraryAnalytics} from '@/models/library-analytics-models';

export class LibraryAnalyticsService {
    private configsService = new ConfigsService();

    async getStats(days: ComparisonDays, source: AnalyticsSource): Promise<LibraryAnalytics> {
        const response = await axios.get<{data: LibraryAnalytics}>(
            `${this.configsService.backendUrl()}/stats/library`,
            {params: source === 'ALL' ? {days} : {days, source}}
        );
        if (!response.data.data) throw new Error('Library statistics are unavailable');
        return response.data.data;
    }
}
