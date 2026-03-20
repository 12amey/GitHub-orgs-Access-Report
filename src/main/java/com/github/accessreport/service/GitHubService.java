package com.github.accessreport.service;

import com.github.accessreport.model.RepoAccess;

import java.util.List;
import java.util.Map;

/**
 * Service interface for GitHub access report operations.
 */
public interface GitHubService {

    /**
     * Builds a complete access report for the given GitHub organization.
     * The report maps each user login to the list of repositories they can access,
     * along with their permission level on each.
     *
     * @param org the GitHub organization login name
     * @return map of username → list of RepoAccess (repo + permission)
     */
    Map<String, List<RepoAccess>> buildAccessReport(String org);
}
