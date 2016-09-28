import esClient from '../lib/esClient'

const defaultOpts = {
  index: 'hakukohde',
  type: 'hakukohde',
}

const composeHakukohde = oid =>
  esClient.getSource({
    ...defaultOpts,
    id: oid,
  }).then(hakukohdeEntity => new Promise((resolve) => {
    const koulutusDocs = hakukohdeEntity.koulutukset.map(k => ({
      _index: 'koulutus',
      _type: 'koulutus',
      _id: k.oid,
    }))
    const hakuDoc = {
      _index: 'haku',
      _type: 'haku',
      _id: hakukohdeEntity.hakuOid,
    }

    esClient.mget({
      body: {
        docs: [hakuDoc, ...koulutusDocs],
      },
    }).then((response) => {
      const composedHakukohde = {
        ...hakukohdeEntity,
        koulutusDocs: [],
      }

      response.docs.forEach((doc) => {
        const entity = doc._source

        switch (doc._type) {
          case 'haku':
            composedHakukohde.hakuDoc = entity
            break
          case 'koulutus':
            composedHakukohde.koulutusDocs.push(entity)
            break
          default:
            throw new Error('Unexpected document type!')
        }
      })

      resolve(composedHakukohde)
    })
  }))


export default composeHakukohde
