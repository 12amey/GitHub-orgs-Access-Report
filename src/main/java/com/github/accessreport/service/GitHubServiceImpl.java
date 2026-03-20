package com.github.accessreport.service;

import com.github.accessreport.client.GitHubApiClient;
import com.github.accessreport.exception.GitHubApiException;
import com.github.accessreport.model.CollaboratorInfo;
import com.github.accessreport.model.RepoAccess;
import com.github.accessreport.model.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Core service that orchestrates data retrieval and aggregation.
 *
 * <p><b>Performance strategy:</b>
 * 1. Fetch all org repositories (sequentially, paginated).
 * 2. Fire off one {@link CompletableFuture} per repository to fetch collaborators in parallel.
 * 3. Join all futures and aggregate results into a user → repos map.
 *
 * <p>This avoids the O(N) sequential latency of per-repo API calls and is safe for
 * 100+ repos / 1000+ users at the cost of bounded thread pool concurrency.
 */
@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);

    private final GitHubApiClient apiClient;
    private final Executor executor;

    public GitHubServiceImpl(GitHubApiClient apiClient,
                              @Qualifier("githubTaskExecutor") Executor executor) {
        this.apiClient = apiClient;
        this.executor = executor;
    }

    @Override
    public Map<String, List<RepoAccess>> buildAccessReport(String org) {
        log.info("Building access report for organization: {}", org);

        // Step 1: Fetch all repositories for the org
        List<RepositoryInfo> repositories = apiClient.getOrgRepositories(org);

        if (repositories.isEmpty()) {
            log.warn("No repositories found for organization: {}", org);
            return Map.of();
        }

        log.info("Found {} repositories for org '{}'. Fetching collaborators in parallel...",
                repositories.size(), org);

        // Step 2: Launch parallel futures — one per repository
        List<CompletableFuture<RepoCollaborators>> futures = repositories.stream()
                .map(repo -> CompletableFuture
                        .supplyAsync(() -> fetchCollaborators(org, repo), executor)
                        .exceptionally(ex -> {
                            log.warn("Failed to fetch collaborators for {}/{}: {}",
                                    org, repo.getName(), ex.getMessage());
                            // Return empty result so one failure doesn't abort the whole report
                            return new RepoCollaborators(repo.getName(), List.of());
                        }))
                .collect(Collectors.toList());

        // Step 3: Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Step 4: Aggregate repo→users into user→repos (ConcurrentHashMap for thread safety)
        Map<String, List<RepoAccess>> accessReport = new ConcurrentHashMap<>();

        futures.forEach(future -> {
            RepoCollaborators result = future.join();
            for (CollaboratorInfo collaborator : result.collaborators()) {
                String username = collaborator.getLogin();
                String permission = collaborator.getHighestPermission();

                accessReport
                        .computeIfAbsent(username, k -> new ArrayList<>())
                        .add(new RepoAccess(result.repoName(), permission));
            }
        });

        log.info("Access report complete for '{}': {} users across {} repositories",
                org, accessReport.size(), repositories.size());

        return accessReport;
    }

    /**
     * Wraps the API call to fetch collaborators for a single repo.
     * Wrapped in a record for use in the parallel stream pipeline.
     */
    private RepoCollaborators fetchCollaborators(String org, RepositoryInfo repo) {
        try {
            List<CollaboratorInfo> collaborators = apiClient.getRepoCollaborators(org, repo.getName());
            return new RepoCollaborators(repo.getName(), collaborators);
        } catch (GitHubApiException ex) {
            // Re-throw so exceptionally() handler can catch and log it gracefully
            throw new RuntimeException(ex);
        }
    }

    /**
     * Immutable record pairing a repo name with its fetched collaborator list.
     */
    private record RepoCollaborators(String repoName, List<CollaboratorInfo> collaborators) {}
}
