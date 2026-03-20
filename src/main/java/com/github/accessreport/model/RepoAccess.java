package com.github.accessreport.model;

/**
 * Represents a single repository access entry for a user,
 * including the repository name and their permission level.
 */
public class RepoAccess {

    private String repo;
    private String permission;

    public RepoAccess() {}

    public RepoAccess(String repo, String permission) {
        this.repo = repo;
        this.permission = permission;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
