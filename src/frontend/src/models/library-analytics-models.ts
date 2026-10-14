export type AnalyticsSource = 'ALL' | 'READER' | 'POCKET';
export type ComparisonDays = 7 | 30 | 90;

export interface LibraryRanking {
    name: string;
    count: number;
    backlog: number;
    archived: number;
}

export interface ComparisonPeriod {
    start: string;
    end: string;
    added: number;
    read: number;
}

export interface ComparisonChange {
    delta: number;
    percent: number | null;
}

export interface LibraryAnalytics {
    asOf: string;
    source: 'READER' | 'POCKET' | null;
    backlog: {
        total: number;
        ageBuckets: {key: string; label: string; count: number}[];
        unknownAge: number;
    };
    archiveDelay: {
        medianDays: number | null;
        eligibleRecords: number;
        excludedRecords: number;
    };
    comparison: {
        days: ComparisonDays;
        current: ComparisonPeriod;
        previous: ComparisonPeriod;
        added: ComparisonChange;
        read: ComparisonChange;
    };
    categories: LibraryRanking[];
    domains: LibraryRanking[];
    unknownDomainCount: number;
}
