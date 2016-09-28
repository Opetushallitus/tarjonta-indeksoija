import indexKoulutus from './indexers/indexKoulutus'

const koulutusOid = process.argv[2]

indexKoulutus(koulutusOid)
  .then(response => console.log(response))
  .catch(e => console.error(e))

