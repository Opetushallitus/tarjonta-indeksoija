import fetch from 'node-fetch'
import _ from 'lodash'
import config from '../config'

const mapOid = tulokset => tulokset.map(t => t.oid)

const mappers = {
  haku: (entity, queryString) =>
    fetch(`${config.baseUrl}/${entity}/find?${queryString}`)
      .then(response => response.json())
      .then(response => mapOid(response.result)),

  generic: (entity, queryString) =>
    fetch(`${config.baseUrl}/${entity}/search?${queryString}`)
      .then(response => response.json())
      .then(response => response.result.tulokset)
      .then(tuloksetByOrg => tuloksetByOrg.map(orgTulos => mapOid(orgTulos.tulokset)))
      .then(_.flatten)
}

const tarjontaSearch = (entity, queryString) =>
  (mappers[entity] || mappers.generic)(entity, queryString)

export default tarjontaSearch
