/*
 * npm install bench-rest
 *
 * Usage instructions and how to interpret test results available at:
 * https://github.com/jeffbski/bench-rest
 * */

var benchrest = require('bench-rest'),
    baseUrl = process.argv[2];

if (process.argv.length < 3) {
    console.log('Usage: node rest_koulutus_perf_test.js <service-url> oid1 oid2 oid3.\nUse "localhost" for default url');
    process.exit(1);
}

if (baseUrl === 'localhost') baseUrl = 'http://localhost:3000/tarjonta-indeksoija';

var requests = {
    main: [
        // Queries for fairly common letters that should appear often in all documents.
        {get: baseUrl + '/api/ui/search?query=a'},
        {get: baseUrl + '/api/ui/search?query=i'},
        {get: baseUrl + '/api/ui/search?query=t'},
        {get: baseUrl + '/api/ui/search?query=n'}
    ]
};

var runOptions = {
    limit: 10,     // concurrent connections
    iterations: 1000
};

benchrest(requests, runOptions)
    .on('error', function (err, ctxName) {
        console.error('Failed in %s with err: ', ctxName, err);
    })
    .on('end', function (stats, errorCount) {
        console.log('error count: ', errorCount);
        console.log('stats', stats);
    });