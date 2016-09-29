import eachSeries from 'async/eachSeries'
import ProgressBar from 'progress'
import tarjontaSearch from './lib/tarjontaSearch'
import indexKoulutus from './indexers/indexKoulutus'

const endpointWithQueryString = process.argv[2]

tarjontaSearch(endpointWithQueryString).then((matchingOids) => {
  const progressBar = new ProgressBar(':bar', { total: matchingOids.length })
  eachSeries(matchingOids, (oid, done) => {
    progressBar.tick()
    indexKoulutus(oid)
      .then(() => done())
      .catch(e => done(e))
  }, (error) => {
    if (error) {
      console.error(error)
    } else {
      console.log('All indexed')
    }
  })
})
