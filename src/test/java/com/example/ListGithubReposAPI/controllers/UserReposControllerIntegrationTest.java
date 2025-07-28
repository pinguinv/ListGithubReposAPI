package com.example.ListGithubReposAPI.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserReposControllerIntegrationTest {

    static ObjectMapper mapper;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/getGithubUserRepos/";
    }

    @Test
    void givenValidUser_whenGetRepos_thenVerifyReposArray() throws JsonProcessingException {
        // Given
        String user = "octocat";
        String url = getBaseUrl() + user;

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody);

        JsonNode reposArray = mapper.readTree(responseBody);

        // Someone might not have any repositories - it is acceptable
        if (reposArray.isEmpty()) return;

        JsonNode firstRepo = reposArray.get(0);

        assertTrue(firstRepo.hasNonNull("repositoryName"));
        assertTrue(firstRepo.hasNonNull("ownerLogin"));
        assertTrue(firstRepo.hasNonNull("branches"));
        assertTrue(firstRepo.get("branches").isArray());
    }
}