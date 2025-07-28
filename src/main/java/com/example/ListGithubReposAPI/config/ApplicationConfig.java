package com.example.ListGithubReposAPI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "github.access")
@Configuration("application")
public class ApplicationConfig {
    
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String githubAccessToken) {
        this.token = githubAccessToken;
    }
}
