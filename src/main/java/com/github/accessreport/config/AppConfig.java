package com.github.accessreport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Application-level configuration:
 * - RestTemplate with GitHub Authorization header injected on every request.
 * - Shared thread pool executor for parallel CompletableFuture tasks.
 */
@Configuration
public class AppConfig {

    private final GitHubProperties gitHubProperties;

    public AppConfig(GitHubProperties gitHubProperties) {
        this.gitHubProperties = gitHubProperties;
    }

    /**
     * RestTemplate with Authorization and Accept headers pre-configured.
     * Using an interceptor ensures every API call carries the PAT automatically.
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            request.getHeaders().set("Authorization", "Bearer " + gitHubProperties.getToken());
            request.getHeaders().set("Accept", "application/vnd.github+json");
            request.getHeaders().set("X-GitHub-Api-Version", "2022-11-28");
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(authInterceptor));
        return restTemplate;
    }

    /**
     * Thread pool for parallel collaborator fetching across repositories.
     * Pool size is configurable via github.thread-pool-size (default: 20).
     */
    @Bean(name = "githubTaskExecutor")
    public Executor githubTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(gitHubProperties.getThreadPoolSize());
        executor.setMaxPoolSize(gitHubProperties.getThreadPoolSize() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("github-fetch-");
        executor.initialize();
        return executor;
    }
}
