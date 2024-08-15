FROM docker.elastic.co/elasticsearch/elasticsearch:8.15.0

ADD decompound /usr/share/elasticsearch/config/decompound

ADD hunspell /usr/share/elasticsearch/config/hunspell

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install https://github.com/Opetushallitus/elasticsearch-analysis-raudikko/releases/download/v0.1/elasticsearch-analysis-raudikko-0.1-es8.15.0.zip
