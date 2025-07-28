package com.example.ListGithubReposAPI.services;

import com.example.ListGithubReposAPI.config.ApplicationConfig;
import com.example.ListGithubReposAPI.models.RepoBranch;
import com.example.ListGithubReposAPI.models.RepoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserReposService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ExecutorService executorService;

    @Autowired
    private ApplicationConfig applicationConfig;

    public UserReposService() {
        restTemplate = new RestTemplate();
        mapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(10);

        // Configure timeouts to avoid hanging requests
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setConnectTimeout(5000); // 5s
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(5000); // 5s
    }

    private Callable<RepoInfo> getRepoInfoCallable(String ownerLogin, String repoName, HttpEntity<String> entity) {
        return () -> {
            String repoBranchesUrl = "https://api.github.com/repos/" + ownerLogin + "/" + repoName + "/branches";

            try {
                List<RepoBranch> branchesList = new ArrayList<>();

                String repoBranchesResponse = restTemplate.exchange(repoBranchesUrl, HttpMethod.GET, entity, String.class).getBody();
                JsonNode branches = mapper.readTree(repoBranchesResponse);

                for (JsonNode branch : branches) {
                    String branchName = branch.get("name").asText();
                    String lastCommitSha = branch.get("commit").get("sha").asText();

                    branchesList.add(new RepoBranch(branchName, lastCommitSha));
                }

                return new RepoInfo(repoName, ownerLogin, branchesList);
            } catch (HttpClientErrorException e) {
                Logger.getLogger("RepoInfoLogger").log(Level.WARNING,
                        "Failed to fetch branches for repo " + repoName + ": " + e.getStatusCode() + " " + e.getMessage());

                return new RepoInfo(repoName, ownerLogin, List.of());
            }
        };
    }

    private ResponseEntity<Map<String, Object>> getErrorResponse(HttpStatus httpStatus, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", httpStatus.value());
        errorResponse.put("message", message);

        return ResponseEntity.status(httpStatus.value()).body(errorResponse);
    }

    public ResponseEntity<?> getGithubUserRepos(String user) {
        if (user.isBlank()) {
            return getErrorResponse(HttpStatus.NOT_FOUND, "User '" + user + "' not found");
        }

        String githubApiUrl = "https://api.github.com/users/" + user + "/repos?type=all";

        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + applicationConfig.getToken());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<RepoInfo> userRepos = new ArrayList<>();

        try {
            // Make GET request
            String response = restTemplate.exchange(githubApiUrl, HttpMethod.GET, entity, String.class).getBody();
            JsonNode repos = mapper.readTree(response);

            List<Callable<RepoInfo>> tasks = new ArrayList<>();

            for (JsonNode repo : repos) {
                boolean isFork = repo.get("fork").asBoolean();

                if (isFork) continue;

                String ownerLogin = repo.get("owner").get("login").asText();
                String repoName = repo.get("name").asText();

                // Run requests for branches concurrently
                tasks.add(getRepoInfoCallable(ownerLogin, repoName, entity));
            }

            List<Future<RepoInfo>> futures = executorService.invokeAll(tasks);

            for (Future<RepoInfo> future : futures)
                userRepos.add(future.get());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return getErrorResponse(HttpStatus.NOT_FOUND, "User '" + user + "' not found");
            }

            throw e;

        } catch (Exception e) {
            return getErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while fetching repositories: " + e.getMessage());
        }

        return ResponseEntity.ok(userRepos);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
