FROM docker.elastic.co/elasticsearch/elasticsearch:7.16.2

ADD decompound /usr/share/elasticsearch/config/decompound
RUN chmod -R 755 /usr/share/elasticsearch/config/decompound
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/config/decompound

ADD hunspell /usr/share/elasticsearch/config/hunspell
RUN chmod -R 755 /usr/share/elasticsearch/config/hunspell
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/config/hunspell

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install https://github.com/Opetushallitus/elasticsearch-analysis-raudikko/releases/download/v0.1/elasticsearch-analysis-raudikko-0.1-es7.16.2.zip
