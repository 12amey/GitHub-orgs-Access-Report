package com.github.accessreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds GitHub-related configuration from application.yml.
 * The token can be injected from an environment variable via ${GITHUB_TOKEN}.
 */
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /** GitHub Personal Access Token. Set via GITHUB_TOKEN env var. */
    private String token;

    /** GitHub REST API base URL. */
    private String baseUrl = "https://api.github.com";

    /** Number of items to request per page (max 100 for GitHub API). */
    private int perPage = 100;

    /** Thread pool size for parallel collaborator fetching. */
    private int threadPoolSize = 20;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
