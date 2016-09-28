import elasticsearch from 'elasticsearch'

const esClient = new elasticsearch.Client({
  host: 'localhost:9200',
})

export default esClient
