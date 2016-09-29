import indexKoulutus from '../indexers/indexKoulutus'
import indexHakukohde from '../indexers/indexHakukohde'
import indexHaku from '../indexers/indexHaku'

const indexers = {
  haku: indexHaku,
  koulutus: indexKoulutus,
  hakukohde: indexHakukohde,
}

const throwIndexerNotFound = () => new Error('Indexer not found!')

const pickIndexer = entity =>
  indexers[entity] || throwIndexerNotFound()

export default pickIndexer
