// Run with Playwright MCP browser_run_code_unsafe(filename=absolute path).
// Serve the production build at http://127.0.0.1:4173. All API calls are mocked.
async (page) => {
  await page.unrouteAll({behavior: 'wait'});
  const errors = [];
  const requests = [];
  let empty = false;
  const onError = error => errors.push(error.message);
  page.on('pageerror', onError);
  const assertEqual = (actual, expected, message) => {
    if (JSON.stringify(actual) !== JSON.stringify(expected)) {
      throw new Error(`${message}: ${JSON.stringify(actual)}`);
    }
  };

  await page.route('https://pocketstats.cybershu.eu/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    let data;
    switch (path) {
      case '/sync/availability':
        data = {available: false};
        break;
      case '/sync/last/READER':
        data = {date: null, records: 0};
        break;
      case '/stats/byDay': {
        const query = request.postDataJSON();
        requests.push(query);
        data = {
          type: query.type,
          stats: empty ? [] : query.type === 'ARCHIVED'
              ? [{day: '2026-10-14', number: 3}, {day: '2026-10-12', number: 8}]
              : [{day: '2026-10-13', number: 15}, {day: '2026-10-12', number: 21}]
        };
        break;
      }
      case '/stats/byPeriods':
        data = {itemsStats: [
          {nameShort: 'week', nameDesc: 'This week',
            period: {begin: '2026-10-12', end: '2026-10-14'}, stats: {added: 36, read: 11}},
          {nameShort: 'all', nameDesc: 'All time', period: null, stats: {added: 1234, read: 567}}
        ]};
        break;
      case '/stats/topTags':
        data = {tags: [], count: 0};
        break;
      case '/stats/langs':
        data = {langCount: {en: 12}};
        break;
      case '/stats/heatmap':
        data = {items: Array.from({length: 168}, (_, index) => ({
          weekday: Math.floor(index / 24) + 1, hour: index % 24, count: 2
        }))};
        break;
      default:
        throw new Error(`Unexpected API request: ${path}`);
    }
    await route.fulfill({status: 200, contentType: 'application/json',
      body: JSON.stringify({status: 0, data})});
  });

  // Read the actual Chart.js rendering state, rather than only Vue input props.
  const readChart = () => {
    const seen = new Set();
    let result;
    function visit(node) {
      if (!node || typeof node !== 'object' || seen.has(node)) return;
      seen.add(node);
      if (node.component) {
        const component = node.component;
        const chart = component.exposed?.chart?.value || component.exposed?.chart;
        if (chart?.config?.type === 'line') {
          result = {
            labels: chart.data.labels,
            series: chart.data.datasets.map(series => ({label: series.label, values: series.data})),
            maximum: chart.scales.y.max,
            size: {width: chart.width, height: chart.height},
            settled: chart.data.datasets.every((series, index) =>
              chart.getDatasetMeta(index).data.every((point, pointIndex) =>
                Math.abs(point.y - chart.scales[series.yAxisID].getPixelForValue(series.data[pointIndex])) < 0.5))
          };
        }
        visit(component.subTree);
      }
      if (Array.isArray(node.children)) node.children.forEach(visit);
    }
    visit(document.querySelector('#app')._vnode);
    return result?.settled ? result : undefined;
  };

  try {
    await page.setViewportSize({width: 1280, height: 900});
    await page.goto('http://127.0.0.1:4173');
    await page.getByRole('cell', {name: '1234', exact: true}).waitFor();
    await page.waitForFunction(readChart);
    const chart = await page.evaluate(readChart);
    assertEqual(chart.labels, ['2026-10-12', '2026-10-13', '2026-10-14'], 'Date order');
    assertEqual(chart.series, [
      {label: 'Read items in day', values: [8, 0, 3]},
      {label: 'Added items in day', values: [21, 15, 0]}
    ], 'Added/Read values and missing-day zeros');
    if (chart.maximum < 21 || chart.size.width <= 0 || chart.size.height <= 0) {
      throw new Error('Chart is clipped or invisible');
    }
    const rows = await page.locator('table tbody tr').evaluateAll(rows =>
      rows.map(row => [...row.querySelectorAll('td')].map(cell => cell.innerText)));
    assertEqual(rows, [
      ['This week', '12-10-2026 - 14-10-2026', '36', '11'],
      ['All time', '', '1234', '567']
    ], 'Period table columns and values');

    // Datepicker selection followed by Apply must retain the chosen period.
    await page.locator('#formDayStart').click();
    await page.getByRole('button', {name: '08', exact: true}).click();
    const chosenStart = await page.locator('#formDayStart').inputValue();
    const navigations = [];
    const onNavigation = frame => {
      if (frame === page.mainFrame()) navigations.push(frame.url());
    };
    page.on('framenavigated', onNavigation);
    const request = page.waitForRequest(request => request.url().endsWith('/stats/byDay')
        && request.postDataJSON().start === chosenStart);
    await page.getByRole('button', {name: 'Apply', exact: true}).click();
    await request;
    await page.waitForLoadState('networkidle');
    page.off('framenavigated', onNavigation);
    assertEqual(navigations, [], 'Apply must not reload the page');
    assertEqual(await page.locator('#formDayStart').inputValue(), chosenStart, 'Selected start retained');

    await page.waitForFunction(readChart);
    await page.screenshot({path: '/tmp/pocketstats-added-read-desktop.png', fullPage: true});
    await page.setViewportSize({width: 390, height: 844});
    await page.reload();
    await page.getByRole('cell', {name: '1234', exact: true}).waitFor();
    await page.waitForFunction(readChart);
    if ((await page.evaluate(readChart)).size.width <= 0) throw new Error('Mobile chart invisible');
    if (!(await page.getByRole('cell', {name: '1234', exact: true}).isVisible())) {
      throw new Error('Mobile period table invisible');
    }
    const tableBounds = await page.locator('table').boundingBox();
    if (tableBounds.x < 0 || tableBounds.x + tableBounds.width > 390) {
      throw new Error('Mobile period table overflows the screen');
    }
    await page.screenshot({path: '/tmp/pocketstats-added-read-mobile.png', fullPage: true});

    empty = true;
    const emptyResponse = page.waitForResponse(response => response.url().endsWith('/stats/byDay'));
    await page.getByRole('button', {name: 'Apply', exact: true}).click();
    await emptyResponse;
    await page.waitForLoadState('networkidle');
    const emptyChart = await page.evaluate(readChart);
    assertEqual(emptyChart.labels, [], 'Empty data clears previous labels');
    assertEqual(emptyChart.series.map(series => series.values), [[], []], 'Empty data clears previous values');

    assertEqual(errors, [], 'JavaScript errors');
    return {passed: ['Added/Read chart', 'Sparse dates', 'Period table', 'Apply without reload',
      'Selected date retained', 'Empty results', 'Mobile visibility'], statisticsRequests: requests.length};
  } finally {
    page.off('pageerror', onError);
    await page.unrouteAll({behavior: 'wait'});
  }
}
