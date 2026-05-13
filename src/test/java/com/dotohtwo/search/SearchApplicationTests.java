package com.dotohtwo.search;

import com.dotohtwo.search.model.Reviewable;
import com.dotohtwo.search.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchApplicationTests {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(elasticsearchOperations, "https://api.themoviedb.org/3");
    }

    @Test
    void searchUsers_returnsElasticsearchResults() {
        User user = new User();
        user.setId("1");
        user.setUsername("alice");

        SearchHit<User> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(user);

        SearchHits<User> hits = mock(SearchHits.class);
        when(hits.stream()).thenReturn(List.of(hit).stream());
        when(elasticsearchOperations.search(any(Query.class), eq(User.class))).thenReturn(hits);

        List<User> results = searchService.searchUsers("alice", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void searchReviewables_returnsElasticsearchResults_whenAboveThreshold() {
        List<Reviewable> reviewables = buildReviewables(6);
        SearchHits<Reviewable> hits = mockReviewableHits(reviewables);
        when(elasticsearchOperations.search(any(Query.class), eq(Reviewable.class))).thenReturn(hits);

        List<Reviewable> results = searchService.searchReviewables("action", 10);

        assertThat(results).hasSize(6);
    }

    @Test
    void searchReviewables_triggersTmdbFallback_whenBelowThreshold() {
        // ES returns 2 results (below threshold of 5) — TMDB key is blank so fallback returns empty
        List<Reviewable> reviewables = buildReviewables(2);
        SearchHits<Reviewable> hits = mockReviewableHits(reviewables);
        when(elasticsearchOperations.search(any(Query.class), eq(Reviewable.class))).thenReturn(hits);

        List<Reviewable> results = searchService.searchReviewables("obscure", 10);

        // With no TMDB key configured, result stays at ES-only count
        assertThat(results).hasSize(2);
    }

    private List<Reviewable> buildReviewables(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> {
            Reviewable r = new Reviewable();
            r.setId(String.valueOf(i));
            r.setTitle("Title " + i);
            return r;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private SearchHits<Reviewable> mockReviewableHits(List<Reviewable> reviewables) {
        List<SearchHit<Reviewable>> hits = reviewables.stream().map(r -> {
            SearchHit<Reviewable> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(r);
            return hit;
        }).toList();

        SearchHits<Reviewable> searchHits = mock(SearchHits.class);
        when(searchHits.stream()).thenReturn(hits.stream());
        return searchHits;
    }
}
