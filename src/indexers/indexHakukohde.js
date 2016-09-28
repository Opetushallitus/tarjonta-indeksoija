import indexGeneric from './indexGeneric'

const indexHakukohde = oid =>
  indexGeneric(`https://testi.virkailija.opintopolku.fi/tarjonta-service/rest/v1/hakukohde/${oid}`, {
    type: 'hakukohde',
    index: 'hakukohde',
  })

export default indexHakukohde
