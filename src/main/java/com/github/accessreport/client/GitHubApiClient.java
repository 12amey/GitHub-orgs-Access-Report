package com.github.accessreport.client;

import com.github.accessreport.config.GitHubProperties;
import com.github.accessreport.exception.GitHubApiException;
import com.github.accessreport.model.CollaboratorInfo;
import com.github.accessreport.model.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Low-level GitHub API client responsible for HTTP interactions.
 * Handles pagination transparently — all methods return complete lists.
 *
 * Uses {@link RestTemplate} which is pre-configured with the Authorization
 * header interceptor in {@link com.github.accessreport.config.AppConfig}.
 */
@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;

    public GitHubApiClient(RestTemplate restTemplate, GitHubProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Fetches all repositories for the given organization, handling pagination automatically.
     *
     * @param org the GitHub organization login name
     * @return complete list of repositories across all pages
     * @throws GitHubApiException if the API call fails
     */
    public List<RepositoryInfo> getOrgRepositories(String org) {
        String urlTemplate = properties.getBaseUrl()
                + "/orgs/{org}/repos?type=all&per_page={perPage}&page={page}";
        log.info("Fetching repositories for organization: {}", org);
        return fetchAllPages(urlTemplate, org, null,
                new ParameterizedTypeReference<List<RepositoryInfo>>() {});
    }

    /**
     * Fetches all collaborators for a specific repository, handling pagination automatically.
     *
     * @param org  the GitHub organization login name
     * @param repo the repository name
     * @return complete list of collaborators across all pages
     * @throws GitHubApiException if the API call fails
     */
    public List<CollaboratorInfo> getRepoCollaborators(String org, String repo) {
        String urlTemplate = properties.getBaseUrl()
                + "/repos/{org}/{repo}/collaborators?affiliation=all&per_page={perPage}&page={page}";
        log.debug("Fetching collaborators for {}/{}", org, repo);
        return fetchAllPages(urlTemplate, org, repo,
                new ParameterizedTypeReference<List<CollaboratorInfo>>() {});
    }

    /**
     * Generic paginated fetch loop.
     * Continues requesting pages until an empty page or a page smaller than perPage is returned.
     *
     * @param urlTemplate URL template with {org}, optional {repo}, {perPage}, {page} placeholders
     * @param org         organization name
     * @param repo        repository name (null for org-level calls)
     * @param responseType type reference for deserialization
     */
    private <T> List<T> fetchAllPages(String urlTemplate,
                                       String org,
                                       String repo,
                                       ParameterizedTypeReference<List<T>> responseType) {
        List<T> allItems = new ArrayList<>();
        int page = 1;
        int perPage = properties.getPerPage();

        try {
            while (true) {
                String url = buildUrl(urlTemplate, org, repo, perPage, page);
                ResponseEntity<List<T>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, responseType);

                List<T> pageItems = response.getBody();
                if (pageItems == null || pageItems.isEmpty()) {
                    break; // No more data
                }

                allItems.addAll(pageItems);
                log.debug("Fetched page {} ({} items)", page, pageItems.size());

                if (pageItems.size() < perPage) {
                    break; // Last page reached
                }
                page++;
            }
        } catch (HttpClientErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            String context = repo != null ? org + "/" + repo : org;

            throw new GitHubApiException(
                    buildErrorMessage(statusCode, context), statusCode, ex);
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Network error while communicating with GitHub API: " + ex.getMessage(), ex);
        }

        return allItems;
    }

    /**
     * Substitutes URL template placeholders with actual values.
     */
    private String buildUrl(String template, String org, String repo, int perPage, int page) {
        String url = template
                .replace("{org}", org)
                .replace("{perPage}", String.valueOf(perPage))
                .replace("{page}", String.valueOf(page));
        if (repo != null) {
            url = url.replace("{repo}", repo);
        }
        return url;
    }

    private String buildErrorMessage(int statusCode, String context) {
        return switch (statusCode) {
            case 401 -> "Authentication failed. Verify your GITHUB_TOKEN is valid and not expired.";
            case 403 -> "Access forbidden for '" + context + "'. Check token scopes or rate limit.";
            case 404 -> "Resource not found: '" + context + "'. Check organization/repository name.";
            default  -> "GitHub API returned status " + statusCode + " for '" + context + "'.";
        };
    }
}
