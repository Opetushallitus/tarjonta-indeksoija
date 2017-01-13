import indexGeneric from './indexGeneric'
import config from '../config'

const indexHakukohde = oid =>
  indexGeneric(`${config.baseUrl}/hakukohde/${oid}`, {
    type: 'hakukohde',
    index: 'hakukohde',
  }, hakukohdeEntity => ({
    ...hakukohdeEntity,
    koulutusmoduuliToteutusTarjoajatiedot: null,
  }))

export default indexHakukohde
