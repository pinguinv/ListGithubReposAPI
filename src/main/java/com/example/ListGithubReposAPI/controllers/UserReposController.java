package com.example.ListGithubReposAPI.controllers;

import com.example.ListGithubReposAPI.services.UserReposService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserReposController {

    private final UserReposService userReposService;

    public UserReposController(UserReposService userReposService) {
        this.userReposService = userReposService;
    }

    @GetMapping("getGithubUserRepos/{user}")
    public ResponseEntity<?> getUserRepos(@PathVariable String user) {
        return userReposService.getGithubUserRepos(user);
    }

}
