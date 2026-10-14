<template>
  <section data-testid="library-analytics" aria-labelledby="library-heading" :aria-busy="loading" class="card">
    <div class="card-body">
      <h2 id="library-heading" class="h4">Library statistics</h2>
      <p id="analytics-scope" class="small text-muted">Backlog, age, archive delay, categories and domains cover all retained items in the selected source. The period changes only the activity comparison. Deleted items, highlights and notes are excluded.</p>
      <div class="row g-3 mb-3">
        <div class="col-12 col-sm-6 col-lg-4">
          <label for="analytics-source" class="form-label">Source</label>
          <select id="analytics-source" v-model="source" data-testid="analytics-source" class="form-select" :disabled="loading" aria-describedby="analytics-scope" @change="load">
            <option value="ALL">All sources</option><option value="READER">Readwise</option><option value="POCKET">Historical Pocket</option>
          </select>
        </div>
        <div class="col-12 col-sm-6 col-lg-4">
          <label for="analytics-days" class="form-label">Comparison period</label>
          <select id="analytics-days" v-model.number="days" data-testid="analytics-days" class="form-select" :disabled="loading" aria-describedby="comparison-help" @change="load">
            <option :value="7">7 days</option><option :value="30">30 days</option><option :value="90">90 days</option>
          </select>
          <div id="comparison-help" class="form-text">Most recent period against the preceding period of equal length.</div>
        </div>
      </div>
      <p v-if="loading" role="status" class="mb-0">Loading library statistics...</p>
      <div v-else-if="error" role="alert">
        <p>{{ error }}</p>
        <button type="button" data-testid="analytics-retry" class="btn btn-outline-primary" @click="load">Retry</button>
      </div>
      <template v-else-if="stats">
        <p class="small text-muted">Snapshot: {{ dateTime(stats.asOf) }}</p>
        <div class="row g-4">
          <section class="col-12 col-md-6" aria-labelledby="backlog-heading">
            <h3 id="backlog-heading" class="h5">Backlog</h3>
            <p class="mb-1"><strong data-testid="backlog-total" class="fs-3">{{ number(stats.backlog.total) }}</strong> items to read</p>
            <p v-if="stats.backlog.total === 0" class="small text-muted mb-0">No items waiting to be read.</p>
          </section>
          <section class="col-12 col-md-6" aria-labelledby="archive-heading">
            <h3 id="archive-heading" class="h5">Time to archive</h3>
            <p class="mb-1"><strong data-testid="archive-median" class="fs-3">{{ stats.archiveDelay.medianDays === null ? 'No data' : number(stats.archiveDelay.medianDays) + ' days' }}</strong><span v-if="stats.archiveDelay.medianDays !== null"> median</span></p>
            <p class="small mb-1">{{ number(stats.archiveDelay.eligibleRecords) }} eligible records; {{ number(stats.archiveDelay.excludedRecords) }} excluded records.</p>
            <p class="small text-muted mb-0">Time to archive, not reading time. Missing or invalid dates are excluded.</p>
          </section>
          <section class="col-12 col-md-6" aria-labelledby="age-heading">
            <h3 id="age-heading" class="h5">Backlog age</h3>
            <dl class="age-list mb-0">
              <template v-for="bucket in stats.backlog.ageBuckets" :key="bucket.key">
                <dt>{{ bucket.label }}</dt><dd :data-testid="'backlog-age-' + bucket.key">{{ number(bucket.count) }}</dd>
              </template>
              <dt>Unknown age</dt><dd data-testid="backlog-unknown-age">{{ number(stats.backlog.unknownAge) }}</dd>
            </dl>
            <p class="small text-muted mb-0">Full days since addition. Missing or future addition dates have unknown age.</p>
          </section>
          <section class="col-12 col-md-6" aria-labelledby="comparison-heading">
            <h3 id="comparison-heading" class="h5">Activity comparison</h3>
            <p class="small mb-1">Current: {{ period(stats.comparison.current) }}</p>
            <p class="small">Previous: {{ period(stats.comparison.previous) }}</p>
            <div class="table-responsive">
              <table class="table table-sm align-middle">
                <caption class="visually-hidden">Added and Read in consecutive {{ stats.comparison.days }} day periods</caption>
                <thead><tr><th scope="col">Activity</th><th scope="col">Current</th><th scope="col">Previous</th><th scope="col">Change</th></tr></thead>
                <tbody>
                  <tr v-for="metric in metrics" :key="metric">
                    <th scope="row">{{ metric === 'added' ? 'Added' : 'Read' }}</th>
                    <td :data-testid="'comparison-current-' + metric">{{ number(stats.comparison.current[metric]) }}</td>
                    <td :data-testid="'comparison-previous-' + metric">{{ number(stats.comparison.previous[metric]) }}</td>
                    <td :data-testid="'comparison-change-' + metric">{{ signed(stats.comparison[metric].delta) }}<br><small>{{ percent(stats.comparison[metric].percent) }}</small></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p class="small text-muted mb-0">Read counts archiving, including items archived without reading. Period end is excluded.</p>
          </section>
          <section v-for="ranking in rankings" :key="ranking.key" :data-testid="'analytics-' + ranking.key" class="col-12 col-lg-6" :aria-labelledby="ranking.key + '-heading'">
            <h3 :id="ranking.key + '-heading'" class="h5">{{ ranking.title }}</h3>
            <p v-if="stats[ranking.key].length === 0" class="small text-muted">{{ ranking.empty }}</p>
            <div v-else class="table-responsive">
              <table class="table table-sm align-middle ranking-table">
                <caption class="visually-hidden">{{ ranking.title }} across retained items</caption>
                <thead><tr><th scope="col">{{ ranking.label }}</th><th scope="col">Total</th><th scope="col">Backlog</th><th scope="col">Archived</th></tr></thead>
                <tbody><tr v-for="entry in stats[ranking.key]" :key="entry.name"><th scope="row">{{ entry.name }}</th><td>{{ number(entry.count) }}</td><td>{{ number(entry.backlog) }}</td><td>{{ number(entry.archived) }}</td></tr></tbody>
              </table>
            </div>
            <p v-if="ranking.key === 'domains'" class="small text-muted mb-0">Includes newsletter sender domains. Unknown domain: <span data-testid="unknown-domain-count">{{ number(stats.unknownDomainCount) }}</span> items.</p>
          </section>
        </div>
      </template>
    </div>
  </section>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {AnalyticsSource, ComparisonDays, ComparisonPeriod, LibraryAnalytics} from '@/models/library-analytics-models';
import {LibraryAnalyticsService} from '@/services/library-analytics-service';
import {useSyncStore} from '@/store';

const service = new LibraryAnalyticsService();
export default defineComponent({
  name: 'LibraryAnalytics',
  data() {
    return {
      source: 'ALL' as AnalyticsSource, days: 30 as ComparisonDays,
      stats: null as LibraryAnalytics | null, error: null as string | null,
      loading: false, requestId: 0,
      unsubscribeSync: null as (() => void) | null,
      metrics: ['added', 'read'] as const,
      rankings: [
        {key: 'categories' as const, title: 'Categories', label: 'Category', empty: 'No categories in the selected source.'},
        {key: 'domains' as const, title: 'Top 10 domains', label: 'Domain', empty: 'No known domains in the selected source.'}
      ]
    };
  },
  mounted() {
    this.unsubscribeSync = useSyncStore().$subscribe(() => this.load());
    this.load();
  },
  beforeUnmount() {
    this.unsubscribeSync?.();
    this.requestId++;
  },
  methods: {
    async load() {
      const requestId = ++this.requestId;
      this.loading = true;
      this.error = null;
      this.stats = null;
      try {
        const stats = await service.getStats(this.days, this.source);
        if (requestId === this.requestId) this.stats = stats;
      } catch {
        if (requestId === this.requestId) this.error = 'Could not load library statistics. Please try again.';
      } finally {
        if (requestId === this.requestId) this.loading = false;
      }
    },
    number(value: number): string { return value.toLocaleString(undefined, {maximumFractionDigits: 1}); },
    signed(value: number): string { return (value > 0 ? '+' : '') + this.number(value); },
    percent(value: number | null): string { return value === null ? 'No previous baseline' : this.signed(value) + '%'; },
    dateTime(value: string): string { return new Date(value).toLocaleString(); },
    period(value: ComparisonPeriod): string { return this.dateTime(value.start) + ' to ' + this.dateTime(value.end); }
  }
});
</script>

<style scoped>
.age-list { display: grid; grid-template-columns: 1fr auto; column-gap: 1rem; }
.age-list dt { font-weight: normal; }
.age-list dd { text-align: right; }
.ranking-table th[scope="row"] { font-weight: normal; overflow-wrap: anywhere; }
.ranking-table td { white-space: nowrap; }
</style>
