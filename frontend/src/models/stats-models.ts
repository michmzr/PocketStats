export interface IDayStat {
    day: Date,
    number: number;
}

export class DayStat implements IDayStat {
    day: Date;
    number: number;

    constructor(day: Date, number: number) {
        this.day = day;
        this.number = number;
    }
}

export interface IDayStats {
    stats: IDayStat[],
    archived: boolean,
    type: string
}

export class DayStats implements IDayStats {
    archived: boolean;
    stats: IDayStat[];
    type: string;

    constructor(archived: boolean, stats: IDayStat[], type: string) {
        this.archived = archived;
        this.stats = stats;
        this.type = type;
    }
}