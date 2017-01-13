import elasticsearch from 'elasticsearch'
import config from '../config'

const esClient = new elasticsearch.Client({
  host: config.elasticSearchLocation,
})

export default esClient
