import indexGeneric from './indexGeneric'
import config from '../config'

const indexHaku = oid =>
  indexGeneric(`${config.baseUrl}/haku/${oid}`, {
    type: 'haku',
    index: 'haku',
  }, hakuEntity => ({
    ...hakuEntity,
    hakukohdeOids: null,
  }))

export default indexHaku
