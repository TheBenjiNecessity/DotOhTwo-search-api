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
import java.util.function.BiFunction;

@Service
public class SearchService {

    private static final String USERS_KEY = "search:users";
    private static final String REVIEWABLES_KEY = "search:reviewables";
    private static final int LIMIT = 10;

    private final StringRedisTemplate redis;

    public SearchService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public List<User> searchUsers(String text) {
        return scan(USERS_KEY, text, (name, id) -> new User(id, name));
    }

    public List<Reviewable> searchReviewables(String text) {
        return scan(REVIEWABLES_KEY, text, (title, id) -> new Reviewable(id, title));
    }

    private <T> List<T> scan(String key, String text, BiFunction<String, String, T> mapper) {
        List<T> results = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match("*" + text + "*").count(100).build();

        try (Cursor<ZSetOperations.TypedTuple<String>> cursor = redis.opsForZSet().scan(key, options)) {
            while (cursor.hasNext() && results.size() < LIMIT) {
                String member = cursor.next().getValue();
                if (member == null) continue;
                int sep = member.lastIndexOf(':');
                if (sep < 1) continue;
                results.add(mapper.apply(member.substring(0, sep), member.substring(sep + 1)));
            }
        }

        return results;
    }
}
