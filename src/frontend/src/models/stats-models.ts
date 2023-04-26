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

export interface ITopTag {
    name: string,
    count: number
}

export interface ITopTags {
    tags: ITopTag[],
    count: number
}

export interface ILangStats {
    langCount: Map<string, number>
}

export type ItemsStatsAggregated = {
    itemsStats: ItemsStatsPerPeriod[];
};

export type ItemsStatsPerPeriod = {
    nameShort: string;
    nameDesc: string;
    stats: PeriodItemsStats;
    period: TimePeriod;
};

export type PeriodItemsStats = {
    added: number, read: number
};

export type TimePeriod = {
    begin: string //ISO standard
    end: string //ISO standard
}