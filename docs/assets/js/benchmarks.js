(async () => {
  const base = document.currentScript
    ? new URL(document.currentScript.src).origin
    : location.origin;
  const dataBase = base + (location.pathname.startsWith('/fixedformat4j') ? '/fixedformat4j' : '') + '/assets/benchmarks';

  let index;
  try {
    const res = await fetch(dataBase + '/index.json');
    if (!res.ok) return;
    index = await res.json();
  } catch {
    return;
  }

  const versions = index.versions || [];
  if (!versions.length) return;

  const allResults = await Promise.all(
    versions.map(async v => {
      try {
        const res = await fetch(dataBase + '/' + encodeURIComponent(v) + '.json');
        return res.ok ? { version: v, data: await res.json() } : null;
      } catch {
        return null;
      }
    })
  );

  const loaded = allResults.filter(Boolean);
  if (!loaded.length) return;

  const COLORS = ['#2563eb', '#16a34a', '#dc2626', '#d97706', '#7c3aed', '#0891b2'];

  function shortName(benchmark) {
    return benchmark.split('.').pop();
  }

  function buildChart(canvasId, title, versionData, mode, filterPrefix) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const benchmarkNames = [...new Set(
      versionData.flatMap(vd =>
        vd.data
          .filter(r => r.mode === mode && shortName(r.benchmark).toLowerCase().startsWith(filterPrefix))
          .map(r => shortName(r.benchmark))
      )
    )].sort();

    if (!benchmarkNames.length) return;

    const datasets = versionData.map((vd, i) => ({
      label: vd.version,
      backgroundColor: COLORS[i % COLORS.length] + 'cc',
      borderColor: COLORS[i % COLORS.length],
      borderWidth: 1,
      data: benchmarkNames.map(name => {
        const result = vd.data.find(
          r => r.mode === mode && shortName(r.benchmark) === name
        );
        return result ? result.primaryMetric.score : null;
      }),
    }));

    new Chart(canvas, {
      type: 'bar',
      data: { labels: benchmarkNames, datasets },
      options: {
        responsive: true,
        plugins: {
          title: { display: true, text: title },
          tooltip: {
            callbacks: {
              label(ctx) {
                const vd = versionData[ctx.datasetIndex];
                const name = benchmarkNames[ctx.dataIndex];
                const result = vd.data.find(
                  r => r.mode === mode && shortName(r.benchmark) === name
                );
                if (!result) return ctx.dataset.label + ': N/A';
                const score = result.primaryMetric.score.toFixed(3);
                const err = result.primaryMetric.scoreError.toFixed(3);
                const unit = result.primaryMetric.scoreUnit;
                return ctx.dataset.label + ': ' + score + ' ± ' + err + ' ' + unit;
              }
            }
          }
        },
        scales: {
          y: { beginAtZero: true }
        }
      }
    });
  }

  buildChart('chart-load-thrpt',  'load() — Throughput',   loaded, 'thrpt', 'load');
  buildChart('chart-export-thrpt', 'export() — Throughput', loaded, 'thrpt', 'export');
  buildChart('chart-load-avgt',   'load() — Avg Time',     loaded, 'avgt',  'load');
  buildChart('chart-export-avgt', 'export() — Avg Time',   loaded, 'avgt',  'export');
})();
