import express from 'express'
import indexKoulutus from './indexers/indexKoulutus'
import indexHakukohde from './indexers/indexHakukohde'
import indexHaku from './indexers/indexHaku'
import composeHakukohde from './composers/composeHakukohde'

const indexers = {
  haku: indexHaku,
  koulutus: indexKoulutus,
  hakukohde: indexHakukohde,
}
const app = express()

app.get('/index/:entity/:oid', (req, res) => {
  const indexer = indexers[req.params.entity]

  if (!indexer) {
    res.status(400).send(`Indexer for entity ${req.params.entity} not found!`)
  } else {
    indexer(req.params.oid)
      .then(status => res.json(status))
      .catch(() => res.status(500).send('Error indexing!'))
  }
})

const composers = {
  // haku: indexHaku,
  // koulutus: indexKoulutus,
  hakukohde: composeHakukohde,
}
app.get('/:entity/:oid', (req, res) => {
  const composer = composers[req.params.entity]

  composer(req.params.oid)
    .then(composedHakukohde => res.json(composedHakukohde))
    .catch(() => res.status(500).send('Error composing entity!'))
})

app.listen(3000)
