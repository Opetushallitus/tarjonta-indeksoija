## "Katkoton" indeksointi

Jos ElasticSearchin indeksien settingsit/mappingsit tai datan tietomalli muuttuu paljon, voi olla tarve luoda indeksit 
kokonaan uudelleen. Tämä pitää tehdä ns. katkottoman indeksoinnin avulla.

Huom! Kyse ei ole kuitenkaan täysin katkottomasti indeksoinnista, sillä se aiheuttaa sen, että indeksoinnin aikana
koulutukset, toteutukset yms. eivät näy virkailijan etusivulla! Indeksointi on katkoton ainoastaan oppijan puolella.

Huom! Konfo-backendiä ei saa asentaa uudelleen, kun katkoton indeksointi on kesken!

1. Luo kouta-indeksit uudelleen kutsumalla
   
   * [POST /kouta-indeksoija/api/rebuild/indices/kouta](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/post_kouta_indeksoija_api_rebuild_indices_kouta)

   Voit myös luoda uudelleen ihan kaikki indeksit tai vain koodisto- tai eperuste-indeksit

   * [POST /kouta-indeksoija/api/rebuild/indices/koodisto](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/post_kouta_indeksoija_api_rebuild_indices_koodisto)
   * [POST /kouta-indeksoija/api/rebuild/indices/eperuste](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/post_kouta_indeksoija_api_rebuild_indices_eperuste)
   * [POST /kouta-indeksoija/api/rebuild/indices/all](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/post_kouta_indeksoija_api_rebuild_indices_all)

2. Nyt virkailijan etusivulla ei enää näy mitään muuta kuin indeksien luonnin jälkeen tehdyt muutokset. 
Oppijan puolella taas näkyy kaikki kuten ennenkin, mutta uudet muutokset eivät välity sinne.

3. Voit nyt nähdä listauksesta, että oppijan ja virkailijan puoli käyttävät eri indeksejä:

   * [GET /kouta-indeksoija/api/rebuild/indices/list/virkailija](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/get_kouta_indeksoija_api_rebuild_indices_list_virkailija)
   * [GET /kouta-indeksoija/api/rebuild/indices/list/oppija](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/get_kouta_indeksoija_api_rebuild_indices_list_oppija)

   Kaikki indeksit ja aliakset näkyvät listauksessa: [GET /kouta-indeksoija/api/rebuild/indices/list](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/get_kouta_indeksoija_api_rebuild_indices_list)
   
4. Indeksoi kouta-data (ja oppilaitokset) uusiin indekseihin:

   * [POST /kouta-indeksoija/api/kouta/all](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/kouta/post_kouta_indeksoija_api_kouta_all)
   * [POST /kouta-indeksoija/api/queuer/oppilaitokset](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/queuer/post_kouta_indeksoija_api_queuer_oppilaitokset)

   Jos olet luonut uudelleen koodisto- tai eperuste-indeksit, indeksoi myös ne: 

   * [POST /kouta-indeksoija/api/indexer/koodistot](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/indexer/post_kouta_indeksoija_api_indexer_koodistot)
   * [POST /kouta-indeksoija/api/queuer/eperusteet](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/queuer/post_kouta_indeksoija_api_queuer_eperusteet)

   Eperusteiden ja oppilaitosten indeksointi menee sqs-jonojen kautta. Voit tarkastalle viestien määrää "fast"-jonosta kentästä ApproximateNumberOfMessages 
   
   * [GET /kouta-indeksoija/api/admin/queue/status](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/admin/get_kouta_indeksoija_api_admin_queue_status)

5. Kun indeksointi on valmis, sykronoi oppijan ja virkailijan puolen aliakset

   Voit tehdä tämän joko kutsumalla rajapintaa [POST /kouta-indeksoija/api/rebuild/indices/aliases/sync](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/post_kouta_indeksoija_api_rebuild_indices_aliases_sync) 
   tai *asentamalla konfo-backendin uudelleen*! Konfo-backend sykronoi aliakset automaattisesti deployn yhteydessä, jos ne eivät ole synkassa.

6. Voit nyt nähdä listauksista, että oppijan ja virkailijan puoli käyttävät samoja indeksejä.

7. Tämän jälkeen deletoi vanhat indeksit. 

   1. Varmista ensin listasta, että olet poistamassa oikeita indeksejä: [GET /kouta-indeksoija/api/rebuild/indices/list/unused](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/get_kouta_indeksoija_api_rebuild_indices_list_unused)
 
   2. Sen jälkeen voit ajaa deleten: [DELETE /kouta-indeksoija/api/rebuild/indices/unused](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/delete_kouta_indeksoija_api_rebuild_indices_unused)
  
   3. Voit myös poistaa vain haluamasi setin indeksejä: [DELETE /kouta-indeksoija/api/rebuild/indices](http://localhost:8100/kouta-indeksoija/swagger/index.html#!/rebuild/delete_kouta_indeksoija_api_rebuild_indices)
