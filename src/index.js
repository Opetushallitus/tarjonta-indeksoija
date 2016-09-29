import express from 'express'
import pickIndexer from './lib/pickIndexer'
import composeHakukohde from './composers/composeHakukohde'

const app = express()
const errors = {
  internalServerError: res => res.status(500).send('Error indexing!'),
  badRequest: res => res.status(400).send('Bad request!'),
}

app.get('/index/:entity/:oid', (req, res) => {
  try {
    const indexer = pickIndexer(req.params.entity)
    indexer(req.params.oid)
      .then(status => res.json(status))
      .catch(() => errors.internalServerError(res))
  } catch (e) {
    errors.badRequest(res)
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
