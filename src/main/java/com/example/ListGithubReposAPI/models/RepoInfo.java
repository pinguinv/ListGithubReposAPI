package com.example.ListGithubReposAPI.models;

import java.util.List;

public class RepoInfo {
    public String repositoryName;
    public String ownerLogin;
    public List<RepoBranch> branches;

    public RepoInfo(String repositoryName, String ownerLogin, List<RepoBranch> branches) {
        this.repositoryName = repositoryName;
        this.ownerLogin = ownerLogin;
        this.branches = branches;
    }
}
