package com.dotohtwo.search;

import com.dotohtwo.search.model.Reviewable;
import com.dotohtwo.search.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RestClient tmdbClient;

    @Value("${tmdb.api-key:}")
    private String tmdbApiKey;

    @Value("${search.reviewable.fallback-threshold:5}")
    private int fallbackThreshold;

    public SearchService(ElasticsearchOperations elasticsearchOperations,
                         @Value("${tmdb.base-url:https://api.themoviedb.org/3}") String tmdbBaseUrl) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.tmdbClient = RestClient.builder()
                .baseUrl(tmdbBaseUrl)
                .build();
    }

    public List<User> searchUsers(String text, int limit) {
        var query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m.query(text).fields("username", "displayName")))
                .withPageable(PageRequest.of(0, limit))
                .build();

        return elasticsearchOperations.search(query, User.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    public List<Reviewable> searchReviewables(String text, int limit) {
        var query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m.query(text).fields("title", "description", "type", "tags")))
                .withPageable(PageRequest.of(0, limit))
                .build();

        List<Reviewable> results = new ArrayList<>(elasticsearchOperations.search(query, Reviewable.class)
                .stream()
                .map(SearchHit::getContent)
                .toList());

        if (results.size() <= fallbackThreshold) {
            results.addAll(searchTmdb(text, limit - results.size()));
        }

        return results;
    }

    private List<Reviewable> searchTmdb(String text, int limit) {
        if (limit <= 0 || tmdbApiKey.isBlank()) {
            return List.of();
        }

        JsonNode response = tmdbClient.get()
                .uri("/search/multi?query={q}&api_key={k}", text, tmdbApiKey)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("results")) {
            return List.of();
        }

        List<Reviewable> results = new ArrayList<>();
        for (JsonNode node : response.get("results")) {
            if (results.size() >= limit) break;
            String mediaType = node.path("media_type").asText("");
            if (mediaType.equals("person")) continue;

            Reviewable r = new Reviewable();
            r.setId("tmdb-" + node.path("id").asText());
            r.setTitle(node.has("title") ? node.path("title").asText() : node.path("name").asText());
            r.setDescription(node.path("overview").asText(null));
            r.setType(mediaType);
            results.add(r);
        }

        return results;
    }
}
