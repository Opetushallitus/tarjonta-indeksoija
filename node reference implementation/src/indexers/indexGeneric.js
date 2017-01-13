import fetch from 'node-fetch'
import _ from 'lodash'
import esClient from '../lib/esClient'

const indexGeneric = (entityUrl, indexOptions, transformationFn = _.identity) =>
  fetch(entityUrl)
    .then(res => res.json())
    .then((json) => {
      if (json.status === 'OK') {
        return json.result
      }
      throw new Error(json)
    })
    .then(transformationFn)
    .then(entity =>
      esClient.index({
        ...indexOptions,
        id: entity.oid,
        body: entity,
      })
    )

export default indexGeneric
