import fetch from 'node-fetch'
import _ from 'lodash'
import config from '../config'

const tarjontaSearch = (entity, queryString) =>
  fetch(`${config.baseUrl}/${entity}/search?${queryString}`)
    .then(response => response.json())
    .then(response => response.result.tulokset)
    .then(tuloksetByOrg => tuloksetByOrg.map(orgTulos =>
      orgTulos.tulokset.map(t => t.oid)
    ))
    .then(_.flatten)

export default tarjontaSearch
