export interface ISyncStatus {
    date: Date | undefined
    records: number
}

export class SyncStatus implements ISyncStatus {
    date: Date | undefined;
    records: number = 0;

    constructor(date: Date | undefined, records: number) {
        this.date = date;
        this.records = records;
    }
}