package com.dotohtwo.search;

import com.dotohtwo.search.model.Reviewable;
import com.dotohtwo.search.model.User;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private static final String USERS_KEY = "search:users";
    private static final String REVIEWABLES_KEY = "search:reviewables";
    private static final int LIMIT = 10;

    private final StringRedisTemplate redis;

    public SearchService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Searches for users whose username partially matches {@code text}.
     *
     * Expects members in the sorted set to follow the format {@code username:id}.
     */
    public List<User> searchUsers(String text) {
        List<User> results = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match("*" + text + "*").count(100).build();

        try (Cursor<ZSetOperations.TypedTuple<String>> cursor =
                     redis.opsForZSet().scan(USERS_KEY, options)) {
            while (cursor.hasNext() && results.size() < LIMIT) {
                String member = cursor.next().getValue();
                if (member == null) continue;
                int sep = member.lastIndexOf(':');
                if (sep < 1) continue;
                results.add(new User(member.substring(sep + 1), member.substring(0, sep)));
            }
        }

        return results;
    }

    /**
     * Searches for reviewables whose title partially matches {@code text}.
     *
     * Expects members in the sorted set to follow the format {@code title:id}.
     */
    public List<Reviewable> searchReviewables(String text) {
        List<Reviewable> results = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match("*" + text + "*").count(100).build();

        try (Cursor<ZSetOperations.TypedTuple<String>> cursor =
                     redis.opsForZSet().scan(REVIEWABLES_KEY, options)) {
            while (cursor.hasNext() && results.size() < LIMIT) {
                String member = cursor.next().getValue();
                if (member == null) continue;
                int sep = member.lastIndexOf(':');
                if (sep < 1) continue;
                results.add(new Reviewable(member.substring(sep + 1), member.substring(0, sep)));
            }
        }

        return results;
    }
}
