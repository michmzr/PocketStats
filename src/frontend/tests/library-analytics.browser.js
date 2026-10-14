// Playwright MCP browser_run_code_unsafe(filename=absolute path).
// Serve the production build on localhost:4173; every API request is mocked.
async (page) => {
  await page.unrouteAll({behavior: 'wait'});
  const errors = [];
  const requests = [];
  let mode = 'normal';
  let available = false;
  let synced = false;
  let release;
  let hold = true;
  const gate = new Promise(resolve => { release = resolve; });
  const onError = error => errors.push(error.message);
  page.on('pageerror', onError);
  const assert = (condition, message) => { if (!condition) throw new Error(message); };
  const section = page.getByTestId('library-analytics');
  const value = id => page.getByTestId(id).innerText();
  const fixture = (days, source) => {
    const end = new Date('2026-10-14T12:00:00Z');
    const start = new Date(end.getTime() - days * 86400000);
    const before = new Date(start.getTime() - days * 86400000);
    const empty = mode === 'empty';
    const backlog = empty ? 0 : source === 'READER' ? 7 : synced ? 13 : 12;
    return {
      asOf: end.toISOString(), source,
      backlog: {total: backlog, ageBuckets: [
        {key: '0-30', label: '0–30 days', count: empty ? 0 : source === 'READER' ? 4 : synced ? 5 : 4},
        {key: '31-90', label: '31–90 days', count: empty ? 0 : source === 'READER' ? 2 : 3},
        {key: '91-180', label: '91–180 days', count: empty || source === 'READER' ? 0 : 2},
        {key: '181+', label: '181+ days', count: empty || source === 'READER' ? 0 : 2}
      ], unknownAge: empty ? 0 : 1},
      archiveDelay: {medianDays: empty ? null : 8.5, eligibleRecords: empty ? 0 : source === 'READER' ? 6 : 10, excludedRecords: empty ? 0 : source === 'READER' ? 1 : 2},
      comparison: {days,
        current: {start: start.toISOString(), end: end.toISOString(), added: empty ? 0 : synced ? 11 : 8, read: empty ? 0 : 4},
        previous: {start: before.toISOString(), end: start.toISOString(), added: empty ? 0 : 5, read: 0},
        added: {delta: empty ? 0 : synced ? 6 : 3, percent: empty ? null : synced ? 120 : 60},
        read: {delta: empty ? 0 : 4, percent: null}},
      categories: empty ? [] : source === 'READER'
        ? [{name: 'article', count: 7, backlog: 4, archived: 3}, {name: 'email', count: 7, backlog: 3, archived: 4}]
        : [{name: 'article', count: synced ? 16 : 15, backlog: synced ? 8 : 7, archived: 8},
          {name: 'email', count: 9, backlog: 5, archived: 4}],
      domains: empty ? [] : source === 'READER'
        ? [{name: 'example.org', count: 8, backlog: 4, archived: 4}, {name: 'newsletter.example.org', count: 5, backlog: 2, archived: 3}]
        : [{name: 'example.org', count: synced ? 16 : 15, backlog: synced ? 8 : 7, archived: 8},
          {name: 'newsletter.example.org', count: 8, backlog: 4, archived: 4}],
      unknownDomainCount: empty ? 0 : 1
    };
  };
  await page.route('https://pocketstats.cybershu.eu/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    let data = {};
    let status = 200;
    switch (url.pathname) {
      case '/stats/library': {
        const days = Number(url.searchParams.get('days'));
        const source = url.searchParams.get('source');
        requests.push({days, source});
        if (hold) await gate;
        if (mode === 'error') status = 503;
        data = fixture(days, source);
        break;
      }
      case '/sync/availability': data = {available}; break;
      case '/sync/last/READER':
        if (request.method() === 'POST') synced = true;
        data = {date: synced ? '2026-10-14T12:00:00Z' : null, records: synced ? 5 : 0};
        break;
      case '/stats/byDay': data = {stats: [], type: request.postDataJSON().type}; break;
      case '/stats/byPeriods': data = {itemsStats: []}; break;
      case '/stats/topTags': data = {tags: [], count: 0}; break;
      case '/stats/langs': data = {langCount: {en: 1}}; break;
      case '/stats/heatmap': data = {items: Array.from({length: 168}, (_, index) => ({
        weekday: Math.floor(index / 24) + 1, hour: index % 24, count: 1
      }))}; break;
      default: throw new Error(`Unexpected API request: ${url.pathname}`);
    }
    await route.fulfill({status, contentType: 'application/json', body: JSON.stringify({status: 0, data})});
  });
  try {
    await page.setViewportSize({width: 1280, height: 900});
    await page.goto('http://127.0.0.1:4173');
    await section.getByRole('status').waitFor();
    assert(await page.getByTestId('analytics-source').isDisabled(), 'Source active during loading');
    assert(await page.getByTestId('analytics-days').isDisabled(), 'Period active during loading');
    hold = false;
    release();
    await page.getByTestId('backlog-total').filter({hasText: '12'}).waitFor();
    assert(await value('backlog-age-0-30') === '4', 'Young backlog count');
    assert(await value('backlog-age-181+') === '2', 'Old backlog count');
    assert(await value('backlog-unknown-age') === '1', 'Unknown age count');
    assert(/8[.,]5 days/.test(await value('archive-median')), 'Archive median');
    for (const [id, expected] of Object.entries({
      'comparison-current-added': '8', 'comparison-current-read': '4',
      'comparison-previous-added': '5', 'comparison-previous-read': '0'
    })) assert(await value(id) === expected, id);
    assert((await value('comparison-change-added')).includes('+60%'), 'Percentage comparison');
    assert((await value('comparison-change-read')).includes('No previous baseline'), 'Zero previous baseline');
    await page.getByTestId('analytics-categories').getByRole('row').filter({hasText: 'email'}).waitFor();
    await page.getByTestId('analytics-domains').getByRole('row').filter({hasText: 'newsletter.example.org'}).waitFor();
    assert(await value('unknown-domain-count') === '1', 'Unknown domains');
    assert(requests.every(request => request.source === null), 'All sources should omit source parameter');

    await page.getByTestId('analytics-source').selectOption({label: 'Readwise'});
    await page.getByTestId('backlog-total').filter({hasText: '7'}).waitFor();
    await page.getByTestId('analytics-days').selectOption('7');
    await page.getByTestId('backlog-total').filter({hasText: '7'}).waitFor();
    assert(requests.some(request => request.days === 7 && request.source === 'READER'), 'Source and period filter request');

    mode = 'empty';
    await page.getByTestId('analytics-source').selectOption({label: 'Historical Pocket'});
    await page.getByTestId('backlog-total').filter({hasText: '0'}).waitFor();
    assert(await value('archive-median') === 'No data', 'Empty archive median');
    await section.getByText('No categories in the selected source.').waitFor();
    await section.getByText('No known domains in the selected source.').waitFor();
    assert(!/Infinity|NaN/.test(await section.innerText()), 'Invalid numeric output');
    mode = 'error';
    await page.getByTestId('analytics-days').selectOption('90');
    await section.getByRole('alert').waitFor();
    assert(await page.getByTestId('backlog-total').count() === 0, 'Stale data visible after failure');
    mode = 'normal';
    await page.getByTestId('analytics-retry').click();
    await page.getByTestId('backlog-total').filter({hasText: '12'}).waitFor();

    available = true;
    await page.reload();
    await page.getByTestId('backlog-total').filter({hasText: '12'}).waitFor();
    await page.getByRole('button', {name: 'Sync Readwise', exact: true}).click();
    await page.getByTestId('backlog-total').filter({hasText: '13'}).waitFor();
    assert(await value('comparison-current-added') === '11', 'Analytics not refreshed after sync');
    await section.screenshot({path: '/tmp/pocketstats-library-analytics-desktop.png'});

    await page.setViewportSize({width: 390, height: 844});
    await page.reload();
    await page.getByTestId('backlog-total').filter({hasText: '13'}).waitFor();
    const bounds = await section.boundingBox();
    assert(bounds.x >= 0 && bounds.x + bounds.width <= 390, 'Analytics section overflows mobile viewport');
    await section.screenshot({path: '/tmp/pocketstats-library-analytics-mobile.png'});
    assert(errors.length === 0, `JavaScript errors: ${errors.join(', ')}`);
    return {passed: ['Six metrics', 'Loading controls', 'No token required', 'Source/period filters',
      'Empty data', 'Error/retry', 'Refresh after sync', 'Mobile layout'], analyticsRequests: requests.length};
  } finally {
    release();
    page.off('pageerror', onError);
    await page.unrouteAll({behavior: 'wait'});
  }
}
