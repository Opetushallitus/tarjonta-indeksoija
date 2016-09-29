import fetch from 'node-fetch'
import _ from 'lodash'
import config from '../config'

const mapOid = tulokset => tulokset.map(t => t.oid)

const tarjontaSearch = (entity, queryString) => {
  if (entity === 'haku') {
    return fetch(`${config.baseUrl}/${entity}/find?${queryString}`)
      .then(response => response.json())
      .then(response => mapOid(response.result))
  }
  return fetch(`${config.baseUrl}/${entity}/search?${queryString}`)
    .then(response => response.json())
    .then(response => response.result.tulokset)
    .then(tuloksetByOrg => tuloksetByOrg.map(orgTulos => mapOid(orgTulos.tulokset)))
    .then(_.flatten)
}

export default tarjontaSearch
