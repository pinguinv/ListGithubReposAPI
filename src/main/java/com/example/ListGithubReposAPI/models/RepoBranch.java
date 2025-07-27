package com.example.ListGithubReposAPI.models;

public class RepoBranch {
    public String name;
    public String lastCommitSha;

    public RepoBranch(String name, String lastCommitSha) {
        this.name = name;
        this.lastCommitSha = lastCommitSha;
    }
}
