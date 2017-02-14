/*
* Usage instructions and how to interpred test results available at:
* https://github.com/jeffbski/bench-rest
* */

var benchrest = require('bench-rest'),
    baseUrl = process.argv[2] || 'http://localhost:3000/tarjonta-indeksoija';

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

var start = new Date().getTime();

benchrest(requests, runOptions)
  .on('error', function (err, ctxName) { console.error('Failed in %s with err: ', ctxName, err); })
  .on('end', function (stats, errorCount) {
    console.log('error count: ', errorCount);
    console.log('test took:', new Date().getTime() - start);
    console.log('stats', stats);
  });