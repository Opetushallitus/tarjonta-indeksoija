import indexGeneric from './indexGeneric'
import config from '../config'

const _ = require('lodash')

const VALUE = 'value'
const KOODI = 'koodi'
const KOODILIST = 'koodilist'
const KUVAUS = 'kuvaus'
const KUVAUSVALMISTAVA = 'kuvausvalmistava'
const KIELIVALIKOIMA = 'kielivalikoima'


function extractKoodi(k) {
  if (!k) return {}
  return {
    uri: k.uri,
    nimi: {
      fi: _.get(k, 'meta.kieli_fi.nimi'),
      sv: _.get(k, 'meta.kieli_sv.nimi'),
      en: _.get(k, 'meta.kieli_en.nimi')
    }
  }
}
function extractKoodiList(field) {
  return _.chain(field).map(extractKoodi).values();
}

function copyField(nk, ok, fieldInfo) {

  switch (fieldInfo.type) {
    case VALUE:
      nk[fieldInfo.field] = _.get(ok, fieldInfo.field)
      break
    case KOODI:
      nk[fieldInfo.field] = extractKoodi(_.get(ok, fieldInfo.field))
      break
    case KOODILIST:
      nk[fieldInfo.field] = extractKoodiList(_.get(ok, `${fieldInfo.field}.meta`))
      break
    case KUVAUS:
      nk[fieldInfo.field] = _.chain(_.get(ok, `${fieldInfo.field}`)).mapValues('tekstis').value()
      break
    case KUVAUSVALMISTAVA: //FIXME painajainen
//      nk[fieldInfo.field] = _.chain(_.get(ok, `${fieldInfo.field}.kuvaus`)).mapValues('tekstis').value()
      break
    case KIELIVALIKOIMA:
      nk[fieldInfo.field] = _.chain(_.get(ok, `${fieldInfo.field}`)).mapValues('meta').mapValues(extractKoodiList).value()
      break
    default:
      console.log(`Broken field: ${fieldInfo}`)
  }
}

const convertedFields = [
  { field: 'version', type: VALUE },
  { field: 'modified', type: VALUE },
  { field: 'modifiedBy', type: VALUE },
  { field: 'nimi', type: VALUE },
  { field: 'aihees', type: KOODILIST },
  { field: 'koulutustyyppi', type: KOODI },
  { field: 'oid', type: VALUE },
  { field: 'koulutuskoodi', type: KOODI },
  { field: 'koulutusaste', type: KOODI },
  { field: 'koulutusala', type: KOODI },
  { field: 'opintoala', type: KOODI },
  { field: 'tutkinto', type: KOODI },
  { field: 'eqf', type: KOODI },
  { field: 'nqf', type: KOODI },
  { field: 'opintojenLaajuusyksikko', type: KOODI },
  { field: 'koulutuksenLaajuusKoodi', type: KOODI },
  { field: 'toteutustyyppi', type: VALUE },
  { field: 'moduulityyppi', type: VALUE },
  { field: 'komoOid', type: VALUE },
  { field: 'komotoOid', type: VALUE },
  { field: 'organisaatio', type: VALUE },
  { field: 'koulutusohjelma', type: KOODI },
  { field: 'tunniste', type: VALUE },
  { field: 'tila', type: VALUE },
  { field: 'koulutusmoduuliTyyppi', type: VALUE },
  { field: 'suunniteltuKestoArvo', type: VALUE },
  { field: 'suunniteltuKestoTyyppi', type: KOODI },
  { field: 'koulutuksenAlkamiskausi', type: KOODI },
  { field: 'koulutuksenAlkamisvuosi', type: VALUE },
  { field: 'koulutuksenAlkamisPvms', type: VALUE },
  { field: 'opetuskielis', type: KOODILIST },
  { field: 'opetusmuodos', type: KOODILIST },
  { field: 'opetusAikas', type: KOODILIST },
  { field: 'opetusPaikkas', type: KOODILIST },
  { field: 'opintojenLaajuusarvo', type: KOODI },
  { field: 'opetusJarjestajat', type: VALUE },
  { field: 'opetusTarjoajat', type: VALUE },
  { field: 'ammattinimikkeet', type: KOODI },
  { field: 'parents', type: VALUE },
  { field: 'children', type: VALUE },
  { field: 'opintojenMaksullisuus', type: VALUE },
  { field: 'isAvoimenYliopistonKoulutus', type: VALUE },
  { field: 'oppiaineet', type: VALUE },
  { field: 'extraParams', type: VALUE },
  { field: 'sisaltyyKoulutuksiin', type: VALUE },
  { field: 'yhteyshenkilos', type: VALUE },
  { field: 'pohjakoulutusvaatimukset', type: KOODI },
  { field: 'tutkintonimikes', type: KOODILIST },
  { field: 'opintojenRakenneKuvas', type: VALUE },
  { field: 'koulutuksenTunnisteOid', type: VALUE },
  { field: 'johtaaTutkintoon', type: VALUE },
  { field: 'ohjelmas', type: VALUE },
  { field: 'hintaString', type: VALUE },
  { field: 'hinta', type: VALUE },
  { field: 'kuvausKomo', type: KUVAUS },
  { field: 'kuvausKomoto', type: KUVAUS },
  { field: 'sisaltyvatKoulutuskoodit', type: KOODILIST },
  { field: 'kandidaatinKoulutuskoodi', type: KOODI },
  { field: 'koulutuksenTavoitteet', type: VALUE },
  { field: 'tutkintonimike', type: KOODI },
  { field: 'koulutuslaji', type: KOODI },
  { field: 'opintojenLaajuusarvoKannassa', type: VALUE },
  { field: 'tarkenne', type: VALUE },
  { field: 'jarjestavaOrganisaatio', type: VALUE },
  { field: 'pohjakoulutusvaatimus', type: KOODI },
  { field: 'linkkiOpetussuunnitelmaan', type: VALUE },
  { field: 'koulutusohjelmanNimiKannassa', type: VALUE },
  { field: 'opintojenLaajuusPistetta', type: VALUE },
  { field: 'koulutusRyhmaOids', type: VALUE },
  { field: 'opintojaksoOids', type: VALUE },
  { field: 'lukiodiplomit', type: KOODILIST },
  { field: 'kielivalikoima', type: KIELIVALIKOIMA },
  { field: 'uniqueExternalId', type: VALUE },
  { field: 'opinnonTyyppiUri', type: VALUE },
  { field: 'hakijalleNaytettavaTunniste', type: VALUE },
  { field: 'opettaja', type: VALUE },
  { field: 'opintokokonaisuusOid', type: VALUE },
  { field: 'koulutuksenLoppumisPvm', type: VALUE },
  { field: 'tarjoajanKoulutus', type: VALUE },
  { field: 'opintopolkuAlkamiskausi', type: VALUE },

  // FIXME tämä on painajaismainen alaobjekti
  { field: 'valmistavaKoulutus', type: KUVAUSVALMISTAVA },
]

const okToMissFieldNames = _.map(convertedFields, f => f.field).concat([
])

function convertKoulutus(ok) {
  const nk = {}
  convertedFields.forEach(f => copyField(nk, ok, f))

  let logoid = false
  _.keys(ok).forEach((f) => {
    if (okToMissFieldNames.indexOf(f) < 0) {
      logoid = true
      console.log(f)
    }
  })
  if (logoid) console.log(ok.oid)
  return nk
}


const indexKoulutus = oid =>
  indexGeneric(`${config.baseUrl}/koulutus/${oid}`, {
    type: 'koulutus',
    index: 'koulutus',
  }, convertKoulutus)

export default indexKoulutus
