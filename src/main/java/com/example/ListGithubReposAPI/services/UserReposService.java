package com.example.ListGithubReposAPI.services;

import com.example.ListGithubReposAPI.models.RepoBranch;
import com.example.ListGithubReposAPI.models.RepoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class UserReposService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ExecutorService executorService;

    public UserReposService() {
        restTemplate = new RestTemplate();
        mapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(10);

        // Configure timeouts to avoid hanging requests
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setConnectTimeout(5000); // 5s
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(5000); // 5s
    }

    public ResponseEntity<?> getGithubUserRepos(String user) {
        String githubApiUrl = "https://api.github.com/users/" + user + "/repos?type=all";

        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/vnd.github+json");
        headers.set("authorization", "Bearer ${github.access.token}");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<RepoInfo> userRepos = new ArrayList<>();

        try {
            // Make GET request
            String response = restTemplate.getForObject(githubApiUrl, String.class, entity, String.class);
            JsonNode repos = mapper.readTree(response);

            List<Callable<RepoInfo>> tasks = new ArrayList<>();

            for (JsonNode repo : repos) {
                boolean isFork = repo.get("fork").asBoolean();

                if (isFork) continue;

                String repoName = repo.get("name").asText();
                String ownerLogin = repo.get("owner").get("login").asText();

                String repoBranchesUrl = "https://api.github.com/repos/" + user + "/" + repoName + "/branches";

                // Run requests for branches concurrently
                tasks.add(() -> {
                    try {
                        List<RepoBranch> branchesList = new ArrayList<>();

                        String repoBranchesResponse = restTemplate.getForObject(repoBranchesUrl, String.class);
                        JsonNode branches = mapper.readTree(repoBranchesResponse);

                        for (JsonNode branch : branches) {
                            String branchName = branch.get("name").asText();
                            String lastCommitSha = branch.get("commit").get("sha").asText();

                            branchesList.add(new RepoBranch(branchName, lastCommitSha));
                        }

                        return new RepoInfo(repoName, ownerLogin, branchesList);
                    } catch (HttpClientErrorException e) {
                        throw e;
                    }
                });
            }

            List<Future<RepoInfo>> futures = executorService.invokeAll(tasks);

            for (Future<RepoInfo> future : futures)
                userRepos.add(future.get());


        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("User not found");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", 404);
                errorResponse.put("message", "User '" + user + "' not found");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            throw e;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 500);
            errorResponse.put("message", "An error occurred while fetching repositiories: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        return ResponseEntity.ok(userRepos);
    }
}
