import fetch from 'node-fetch'
import _ from 'lodash'
import config from '../config'

const tarjontaSearch = endpointWithQueryString =>
  fetch(`${config.baseUrl}/${endpointWithQueryString}`)
    .then(response => response.json())
    .then(response => response.result.tulokset)
    .then(tuloksetByOrg => tuloksetByOrg.map(orgTulos =>
      orgTulos.tulokset.map(t => t.oid)
    ))
    .then(_.flatten)

export default tarjontaSearch
