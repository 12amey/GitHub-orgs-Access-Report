package com.github.accessreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a GitHub repository returned by the GitHub API.
 */
public class RepositoryInfo {

    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private boolean isPrivate;

    public RepositoryInfo() {}

    public RepositoryInfo(String name, String fullName, boolean isPrivate) {
        this.name = name;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }
}
