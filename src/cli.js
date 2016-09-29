import eachSeries from 'async/eachSeries'
import ProgressBar from 'progress'
import tarjontaSearch from './lib/tarjontaSearch'
import pickIndexer from './lib/pickIndexer'

const entity = process.argv[2]
const queryString = process.argv[3]

tarjontaSearch(entity, queryString).then((matchingOids) => {
  const progressBar = new ProgressBar(':bar', { total: matchingOids.length })
  eachSeries(matchingOids, (oid, done) => {
    progressBar.tick()
    pickIndexer(entity)(oid)
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
