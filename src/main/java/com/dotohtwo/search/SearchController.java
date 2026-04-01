package com.dotohtwo.search;

import com.dotohtwo.search.model.Reviewable;
import com.dotohtwo.search.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/users")
    public List<User> searchUsers(@RequestParam String text,
                                  @RequestParam(defaultValue = "10") int limit) {
        return searchService.searchUsers(text, limit);
    }

    @GetMapping("/reviewables")
    public List<Reviewable> searchReviewables(@RequestParam String text,
                                              @RequestParam(defaultValue = "10") int limit) {
        return searchService.searchReviewables(text, limit);
    }
}
