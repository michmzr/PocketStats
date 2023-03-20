export interface IDayStat {
    day: Date,
    number: number;
}

export enum DayStatsType {
    ARCHIVED = "ARCHIVED",
    TODO = "TODO"
}

export interface IDayStats {
    stats: IDayStat[],
    type: string
}

export class DayStats implements IDayStats {
    stats: IDayStat[];
    type: string;

    constructor(stats: IDayStat[], type: string) {
        this.stats = stats;
        this.type = type;
    }
}