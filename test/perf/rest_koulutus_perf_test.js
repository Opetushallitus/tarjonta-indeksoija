/*
 * npm install bench-rest
 *
 * Usage instructions and how to interpret test results available at:
 * https://github.com/jeffbski/bench-rest
 * */

var benchrest = require('bench-rest'),
    baseUrl = process.argv[2],
    oids = process.argv.slice(3);

if (process.argv.length < 3) {
    console.log('Usage: node rest_koulutus_perf_test.js <service-url> oid1 oid2 oid3.\nUse "localhost" for default url');
    process.exit(1);
}

if (baseUrl === 'localhost') baseUrl = 'http://localhost:3000/tarjonta-indeksoija';

if (oids.length < 1) {
    console.log('Provide a list of oids as script parameters.');
    console.log('Usage: node rest_koulutus_perf_test.js <service-url> oid1 oid2 oid3.\nUse "localhost" for default url');
    process.exit(1);
}

var requests = {
    // Queries for koulutus page
    main: oids.map(function (oid) {
        return {get: baseUrl + '/api/ui/koulutus/' + oid}
    })
};

var runOptions = {
    limit: 10,     // concurrent connections
    iterations: 1000
};

var start = new Date().getTime();

benchrest(requests, runOptions)
    .on('error', function (err, ctxName) {
        console.error('Failed in %s with err: ', ctxName, err);
    })
    .on('end', function (stats, errorCount) {
        console.log('error count: ', errorCount);
        console.log("Started at: " + start + ", end: " + new Date().getTime());
        console.log("Get perf tests statistics from elastic with:\nhttp://localhost:3000/tarjonta-indeksoija/api/admin/performance_info?since=" + start);
        console.log('stats', stats);
    });