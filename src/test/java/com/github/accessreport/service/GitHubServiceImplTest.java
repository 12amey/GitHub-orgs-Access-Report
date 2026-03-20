package com.github.accessreport.service;

import com.github.accessreport.client.GitHubApiClient;
import com.github.accessreport.model.CollaboratorInfo;
import com.github.accessreport.model.RepoAccess;
import com.github.accessreport.model.RepositoryInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubServiceImpl Tests")
class GitHubServiceImplTest {

    @Mock
    private GitHubApiClient apiClient;

    private GitHubServiceImpl service;

    @BeforeEach
    void setUp() {
        // Use a direct executor so CompletableFuture runs synchronously in tests
        service = new GitHubServiceImpl(apiClient, Executors.newFixedThreadPool(4));
    }

    @Test
    @DisplayName("Should aggregate user access across multiple repositories")
    void shouldAggregateUserAccessAcrossRepositories() {
        // Given
        RepositoryInfo repo1 = new RepositoryInfo("backend-api", "org/backend-api", false);
        RepositoryInfo repo2 = new RepositoryInfo("frontend-app", "org/frontend-app", false);

        CollaboratorInfo alice = makeCollaborator("alice", true, false, false);
        CollaboratorInfo bob = makeCollaborator("bob", false, true, false);

        when(apiClient.getOrgRepositories("myorg")).thenReturn(List.of(repo1, repo2));
        when(apiClient.getRepoCollaborators("myorg", "backend-api")).thenReturn(List.of(alice, bob));
        when(apiClient.getRepoCollaborators("myorg", "frontend-app")).thenReturn(List.of(alice));

        // When
        Map<String, List<RepoAccess>> report = service.buildAccessReport("myorg");

        // Then
        assertThat(report).containsKeys("alice", "bob");
        assertThat(report.get("alice"))
                .extracting(RepoAccess::getRepo)
                .containsExactlyInAnyOrder("backend-api", "frontend-app");
        assertThat(report.get("bob"))
                .extracting(RepoAccess::getRepo)
                .containsExactly("backend-api");

        // Permission levels
        assertThat(report.get("alice"))
                .filteredOn(r -> r.getRepo().equals("backend-api"))
                .extracting(RepoAccess::getPermission)
                .containsExactly("admin");
        assertThat(report.get("bob"))
                .extracting(RepoAccess::getPermission)
                .containsExactly("write");
    }

    @Test
    @DisplayName("Should return empty report when organization has no repositories")
    void shouldReturnEmptyReportForEmptyOrg() {
        when(apiClient.getOrgRepositories("empty-org")).thenReturn(List.of());
        Map<String, List<RepoAccess>> report = service.buildAccessReport("empty-org");
        assertThat(report).isEmpty();
    }

    @Test
    @DisplayName("Should return empty report when repositories have no collaborators")
    void shouldHandleReposWithNoCollaborators() {
        RepositoryInfo repo = new RepositoryInfo("lone-repo", "org/lone-repo", false);
        when(apiClient.getOrgRepositories("myorg")).thenReturn(List.of(repo));
        when(apiClient.getRepoCollaborators("myorg", "lone-repo")).thenReturn(List.of());

        Map<String, List<RepoAccess>> report = service.buildAccessReport("myorg");
        assertThat(report).isEmpty();
    }

    @Test
    @DisplayName("Should continue processing if one repo's collaborator fetch fails")
    void shouldContinueOnPartialFailure() {
        RepositoryInfo repo1 = new RepositoryInfo("good-repo", "org/good-repo", false);
        RepositoryInfo repo2 = new RepositoryInfo("bad-repo", "org/bad-repo", false);

        CollaboratorInfo alice = makeCollaborator("alice", false, true, false);

        when(apiClient.getOrgRepositories("myorg")).thenReturn(List.of(repo1, repo2));
        when(apiClient.getRepoCollaborators("myorg", "good-repo")).thenReturn(List.of(alice));
        when(apiClient.getRepoCollaborators("myorg", "bad-repo"))
                .thenThrow(new RuntimeException("Simulated network failure"));

        // Should not throw — partial failure is recoverable
        Map<String, List<RepoAccess>> report = service.buildAccessReport("myorg");

        assertThat(report).containsKey("alice");
        assertThat(report.get("alice"))
                .extracting(RepoAccess::getRepo)
                .containsExactly("good-repo");
    }

    @Test
    @DisplayName("Should correctly derive permission levels")
    void shouldDeriveCorrectPermissionLevels() {
        RepositoryInfo repo = new RepositoryInfo("test-repo", "org/test-repo", false);

        CollaboratorInfo readUser  = makeCollaborator("reader", false, false, true);
        CollaboratorInfo writeUser = makeCollaborator("writer", false, true, false);
        CollaboratorInfo adminUser = makeCollaborator("owner", true, true, true);

        when(apiClient.getOrgRepositories("myorg")).thenReturn(List.of(repo));
        when(apiClient.getRepoCollaborators("myorg", "test-repo"))
                .thenReturn(List.of(readUser, writeUser, adminUser));

        Map<String, List<RepoAccess>> report = service.buildAccessReport("myorg");

        assertThat(report.get("reader").get(0).getPermission()).isEqualTo("read");
        assertThat(report.get("writer").get(0).getPermission()).isEqualTo("write");
        assertThat(report.get("owner").get(0).getPermission()).isEqualTo("admin");
    }

    // -------------------------------------------------------------------------
    // Helper factories
    // -------------------------------------------------------------------------

    private CollaboratorInfo makeCollaborator(String login, boolean admin, boolean push, boolean pull) {
        CollaboratorInfo collaborator = new CollaboratorInfo();
        collaborator.setLogin(login);

        CollaboratorInfo.Permissions permissions = new CollaboratorInfo.Permissions();
        permissions.setAdmin(admin);
        permissions.setPush(push);
        permissions.setPull(pull);
        collaborator.setPermissions(permissions);

        return collaborator;
    }
}
