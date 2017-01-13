import eachLimit from 'async/eachLimit'
import ProgressBar from 'progress'
import tarjontaSearch from './lib/tarjontaSearch'
import pickIndexer from './lib/pickIndexer'

const entity = process.argv[2]
const queryString = process.argv[3]
const parallellIndexingOperations = 6

tarjontaSearch(entity, queryString).then((matchingOids) => {
  console.log(`Indexing ${matchingOids.length} documents`)

  const progressBar = new ProgressBar(':bar', { total: matchingOids.length })

  eachLimit(matchingOids, parallellIndexingOperations, (oid, done) => {
    progressBar.tick()
    pickIndexer(entity)(oid)
      .then(() => done())
      .catch(e => done(e))
  }, (error) => {
    if (error) {
      console.error(error)
    } else {
      console.log('All documents indexed')
    }
  })
})
