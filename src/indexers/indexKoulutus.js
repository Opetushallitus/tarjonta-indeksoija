import indexGeneric from './indexGeneric'
import config from '../config'

const indexKoulutus = oid =>
  indexGeneric(`${config.baseUrl}/koulutus/${oid}`, {
    type: 'koulutus',
    index: 'koulutus',
  })

export default indexKoulutus
