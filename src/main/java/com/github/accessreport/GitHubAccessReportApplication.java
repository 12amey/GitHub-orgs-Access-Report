package com.github.accessreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class GitHubAccessReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubAccessReportApplication.class, args);
    }
}
