package com.github.accessreport.controller;

import com.github.accessreport.model.RepoAccess;
import com.github.accessreport.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the GitHub access report API.
 *
 * <p>Endpoint: {@code GET /api/github/access-report?org={organization}}
 *
 * <p>Returns a JSON object mapping each GitHub username to the list of
 * repositories they can access along with their permission level.
 */
@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private static final Logger log = LoggerFactory.getLogger(GitHubController.class);

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * Returns the access report for the given GitHub organization.
     *
     * @param org the GitHub organization login name (e.g., "microsoft", "spring-projects")
     * @return JSON map of username → [{ repo, permission }]
     *
     * <pre>
     * Example response:
     * {
     *   "alice": [
     *     { "repo": "backend-api", "permission": "admin" },
     *     { "repo": "frontend-app", "permission": "write" }
     *   ],
     *   "bob": [
     *     { "repo": "backend-api", "permission": "read" }
     *   ]
     * }
     * </pre>
     */
    @GetMapping("/access-report")
    public ResponseEntity<Map<String, List<RepoAccess>>> getAccessReport(
            @RequestParam(name = "org") String org) {

        if (!StringUtils.hasText(org)) {
            throw new IllegalArgumentException(
                    "The 'org' parameter must not be blank.");
        }

        String sanitizedOrg = org.trim();
        log.info("Received access report request for org: {}", sanitizedOrg);

        Map<String, List<RepoAccess>> report = gitHubService.buildAccessReport(sanitizedOrg);
        return ResponseEntity.ok(report);
    }
}
