package com.jobscanner.search.adapter.out.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.hc.core5.http.HttpHost;

@Configuration
public class SearchIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.search-index", havingValue = "memory", matchIfMissing = true)
    public JobSearchIndex inMemoryJobIndex() {
        log.info("Using InMemoryJobIndex (app.search-index=memory)");
        return new InMemoryJobIndex();
    }

    @Bean
    @ConditionalOnProperty(name = "app.search-index", havingValue = "opensearch")
    public JobSearchIndex openSearchJobIndex(
            @Value("${app.opensearch.host:localhost}") String host,
            @Value("${app.opensearch.port:9200}") int port,
            @Value("${app.opensearch.scheme:http}") String scheme,
            @Value("${app.opensearch.username:admin}") String username,
            @Value("${app.opensearch.password:admin}") String password) {
        log.info("Using OpenSearchJobIndex (app.search-index=opensearch) → {}://{}:{}", scheme, host, port);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        HttpHost httpHost = new HttpHost(scheme, host, port);
        var transport = ApacheHttpClient5TransportBuilder
                .builder(httpHost)
                .setMapper(new JacksonJsonpMapper(mapper))
                .setHttpClientConfigCallback(b -> b.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        OpenSearchClient client = new OpenSearchClient(transport);
        return new OpenSearchJobIndex(client);
    }
}
