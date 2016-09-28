import indexGeneric from './indexGeneric'

const indexKoulutus = oid =>
  indexGeneric(`https://testi.virkailija.opintopolku.fi/tarjonta-service/rest/v1/koulutus/${oid}`, {
    type: 'koulutus',
    index: 'koulutus',
  })

export default indexKoulutus
