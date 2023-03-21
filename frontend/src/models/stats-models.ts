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