FROM docker.elastic.co/elasticsearch/elasticsearch:7.10.0

COPY sanat.txt /usr/share/elasticsearch/config
RUN chmod 660 /usr/share/elasticsearch/config/sanat.txt
RUN chown elastic /usr/share/elasticsearch/config/sanat.txt
RUN ls -l /tmp

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install https://github.com/EvidentSolutions/elasticsearch-analysis-raudikko/releases/download/v0.1/elasticsearch-analysis-raudikko-0.1-es7.10.0.zip
