package com.github.accessreport.controller;

import com.github.accessreport.exception.GitHubApiException;
import com.github.accessreport.model.RepoAccess;
import com.github.accessreport.service.GitHubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GitHubController.class)
@DisplayName("GitHubController Integration Tests")
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubService gitHubService;

    @Test
    @DisplayName("GET /api/github/access-report returns 200 with access report")
    void shouldReturn200WithAccessReport() throws Exception {
        Map<String, List<RepoAccess>> mockReport = Map.of(
                "alice", List.of(new RepoAccess("backend-api", "admin")),
                "bob",   List.of(new RepoAccess("frontend-app", "write"))
        );

        when(gitHubService.buildAccessReport("myorg")).thenReturn(mockReport);

        mockMvc.perform(get("/api/github/access-report")
                        .param("org", "myorg")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.alice[0].repo").value("backend-api"))
                .andExpect(jsonPath("$.alice[0].permission").value("admin"))
                .andExpect(jsonPath("$.bob[0].repo").value("frontend-app"))
                .andExpect(jsonPath("$.bob[0].permission").value("write"));
    }

    @Test
    @DisplayName("GET /api/github/access-report returns 400 when org is missing")
    void shouldReturn400WhenOrgParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/github/access-report")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/github/access-report returns 404 when org not found on GitHub")
    void shouldReturn404WhenOrgNotFound() throws Exception {
        when(gitHubService.buildAccessReport("unknown-org"))
                .thenThrow(new GitHubApiException("Resource not found: 'unknown-org'.", 404));

        mockMvc.perform(get("/api/github/access-report")
                        .param("org", "unknown-org")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/github/access-report returns 401 when token is invalid")
    void shouldReturn401WhenAuthFails() throws Exception {
        when(gitHubService.buildAccessReport("myorg"))
                .thenThrow(new GitHubApiException("Authentication failed.", 401));

        mockMvc.perform(get("/api/github/access-report")
                        .param("org", "myorg")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/github/access-report returns 200 with empty map for org with no repos")
    void shouldReturn200WithEmptyMapForEmptyOrg() throws Exception {
        when(gitHubService.buildAccessReport("empty-org")).thenReturn(Map.of());

        mockMvc.perform(get("/api/github/access-report")
                        .param("org", "empty-org")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
    }
}
