# Tarjonta/KI indeksoija PoC

## Sovelluksen ajaminen
- `npm install`
- Tarkista asetukset tiedostosta `src/config.js`
- Käynnistä yhdessä terminaalissa `npm run watch`

### Indeksointi
- Indeksointi: `node dist/cli.js {ENTITY} {HAKUEHDOT}`
  - ENTITY on joku näistä: koulutus, hakukohde, haku
  - HAKUEHDOT esim. `tila=VALMIS`, tämä lähetetään sellaisenaan tarjonnan search rajapinnalle

### Rest rajapinta indeksistä hakemiseen
- `node dist/index.js` käynnistää web-palvelimen portissa 3000
- Nyt voi tarkastella hakukohdetta, esim: http://localhost:3000/hakukohde/1.2.246.562.20.557732879610
  - Olettaen, että kyseinen hakukohde on ennestään indeksoitu `node dist/cli.js hakukohde hakukohdeOid=1.2.246.562.20.557732879610`

