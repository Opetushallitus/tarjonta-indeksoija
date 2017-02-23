/*
 * npm install bench-rest
 *
 * Usage instructions and how to interpret test results available at:
 * https://github.com/jeffbski/bench-rest
 * */

var benchrest = require('bench-rest'),
    baseUrl = process.argv[2];
    queries = require('./words.json');

if (process.argv.length < 3) {
    console.log('Usage: node rest_koulutus_perf_test.js <service-url>. Use "localhost" for default url');
    process.exit(1);
}

if (baseUrl === 'localhost') baseUrl = 'http://localhost:3000/tarjonta-indeksoija';

var urls = queries.map(function (query) {
    return {get: baseUrl + '/api/ui/search?query=' + query}
});

var results = {};

var start = new Date().getTime();

urls.forEach(function(url) {
    var requests = {
        main: [url]
    };

    var runOptions = {
        limit: 10,     // concurrent connections
        iterations: 20
    };

    benchrest(requests, runOptions)
        .on('error', function (err, ctxName) {
            console.error('Failed in %s with err: ', ctxName, err);
        })
        .on('end', function (stats, errorCount) {
            results[url.get] = [stats.main.histogram.min, stats.main.histogram.max, stats.main.histogram.mean];
            if (urls.indexOf(url) == urls.length-1) {
                var end = new Date().getTime();
                console.log('error count: ', errorCount);
                console.log('Started at: ' + start + ', end: ' + end + ' total time ' + ((end - start) / 1000) + ' seconds.');
                console.log('Get more perf tests statistics from elastic with:\nhttp://localhost:3000/tarjonta-indeksoija/api/admin/performance_info?since=' + start);
                console.log('Results below are in format <query: [minTime, maxTime, meanTime]>.');
                console.log('The numbers represent the total time for ' + runOptions.iterations + ' requests, not a single request.');
                console.log(results);
            }
        });
});
