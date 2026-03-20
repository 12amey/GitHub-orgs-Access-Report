# GitHub Access Report Service

A Spring Boot REST service that connects to the GitHub API to generate structured access reports for GitHub organizations. It returns a clear mapping of **which users have access to which repositories**, along with their permission levels.

---

## Table of Contents

- [How to Run](#how-to-run)
- [Authentication Configuration](#authentication-configuration)
- [API Endpoint](#api-endpoint)
- [Response Format](#response-format)
- [Error Responses](#error-responses)
- [Running Tests](#running-tests)
- [Design Decisions & Assumptions](#design-decisions--assumptions)
- [Project Structure](#project-structure)

---

## How to Run

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+     |
| Maven | 3.8+   |

### 1. Set your GitHub Personal Access Token

The application requires a [GitHub Personal Access Token (PAT)](https://github.com/settings/tokens) to authenticate with the GitHub API.

**Option A — Environment Variable (Recommended):**

```bash
# Windows (PowerShell)
$env:GITHUB_TOKEN="ghp_your_personal_access_token_here"

# macOS / Linux
export GITHUB_TOKEN=ghp_your_personal_access_token_here
```

**Option B — Edit `application.yml` directly:**

Open `src/main/resources/application.yml` and replace the placeholder:

```yaml
github:
  token: your_github_personal_access_token_here
```

> ⚠️ Never commit your token to version control.

### 2. Build and Run

```bash
cd github-access-report

# Run directly with Maven
mvn spring-boot:run

# Or build the JAR first, then run
mvn clean package
java -jar target/github-access-report-1.0.0.jar
```

The application will start on **port 8080** by default.

---

## Authentication Configuration

The service uses **GitHub Personal Access Token (PAT)** authentication.

**Token Scopes Required:**
- `repo` — for access to private repositories and collaborator data
- `read:org` — to list organization repositories

**How it works:**
Every API request to GitHub includes:
```
Authorization: Bearer <your-token>
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

The token is injected automatically via a `RestTemplate` interceptor defined in `AppConfig`.

---

## API Endpoint

### `GET /api/github/access-report`

Returns a JSON report showing which users have access to which repositories within a GitHub organization.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `org`     | ✅ Yes   | GitHub organization login name (e.g., `microsoft`, `spring-projects`) |

**Example Request:**
```
GET http://localhost:8080/api/github/access-report?org=your-org-name
```

---

## Response Format

**Success — `200 OK`:**

```json
{
  "alice": [
    { "repo": "backend-api",   "permission": "admin" },
    { "repo": "frontend-app",  "permission": "write" }
  ],
  "bob": [
    { "repo": "backend-api",   "permission": "read" }
  ],
  "charlie": [
    { "repo": "frontend-app",  "permission": "write" },
    { "repo": "infra-tools",   "permission": "admin" }
  ]
}
```

**Permission Levels** (from highest to lowest):

| Level      | Meaning                                   |
|------------|-------------------------------------------|
| `admin`    | Full control including settings           |
| `maintain` | Manage repository without admin access    |
| `write`    | Push code, manage issues/PRs              |
| `triage`   | Manage issues and PRs without write       |
| `read`     | Read-only access                          |

---

## Error Responses

All errors return a structured JSON body:

```json
{
  "timestamp": "2026-03-20T09:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found: 'invalid-org'. Check organization/repository name."
}
```

| Status | Cause |
|--------|-------|
| `400 Bad Request`    | Missing or blank `org` parameter |
| `401 Unauthorized`   | Invalid or expired GitHub token |
| `403 Forbidden`      | Insufficient token scopes or rate limit exceeded |
| `404 Not Found`      | Organization does not exist on GitHub |
| `502 Bad Gateway`    | Unexpected GitHub API failure |

---

## Running Tests

```bash
cd github-access-report

# Run all tests
mvn test

# Run only unit tests (service layer)
mvn test -Dtest=GitHubServiceImplTest

# Run only controller tests
mvn test -Dtest=GitHubControllerTest
```

**Test Coverage:**
- ✅ Service: aggregation logic, empty org, partial failures, permission derivation
- ✅ Controller: valid request, missing param, 404, 401, empty result
- ✅ Context load test

---

## Design Decisions & Assumptions

### Parallel Processing (CompletableFuture)
Fetching collaborators for each repository is done in **parallel** using `CompletableFuture` with a configurable thread pool (`github.thread-pool-size`, default: 20). This reduces total latency from O(N × latency) to ≈ O(latency) for 100+ repos.

### Pagination
All GitHub API calls use `?per_page=100` and automatically loop through all pages. Pagination stops when a page returns fewer than 100 items.

### Partial Failure Tolerance
If a single repository's collaborator fetch fails (e.g., 403 on a specific repo), that repo is **skipped with a warning log** and the report continues building for the rest. The service does not fail entirely due to a single repo error.

### Collaborator Affiliation
The API is called with `?affiliation=all`, which includes:
- Direct collaborators (explicitly added)
- Those with access via organization teams

### Permission Derivation
GitHub returns boolean flags (`admin`, `push`, `pull`, `maintain`, `triage`). The service resolves the **highest effective permission** from these flags.

### Assumptions
1. The provided token has `repo` and `read:org` scopes.
2. The organization login name is case-insensitive (GitHub handles this).
3. Outside collaborators (not org members) are included if the token allows.
4. Rate limiting is handled by token scope; no retry logic is implemented (can be added with Spring Retry).

### Possible Enhancements
- **Redis caching** to avoid repeated API calls for the same org
- **Spring Retry** for transient failures and rate limit back-off
- **Scheduled reports** stored in a database for historical comparison
- **Role-based filtering** endpoint: `?permission=admin`

---

## Project Structure

```
github-access-report/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/github/accessreport/
    │   │   ├── GitHubAccessReportApplication.java   # Entry point
    │   │   ├── config/
    │   │   │   ├── AppConfig.java                   # RestTemplate + thread pool beans
    │   │   │   └── GitHubProperties.java            # Configuration properties
    │   │   ├── client/
    │   │   │   └── GitHubApiClient.java             # Paginated GitHub API calls
    │   │   ├── service/
    │   │   │   ├── GitHubService.java               # Interface
    │   │   │   └── GitHubServiceImpl.java           # Parallel aggregation logic
    │   │   ├── controller/
    │   │   │   └── GitHubController.java            # REST API endpoint
    │   │   ├── model/
    │   │   │   ├── RepositoryInfo.java
    │   │   │   ├── CollaboratorInfo.java
    │   │   │   └── RepoAccess.java
    │   │   └── exception/
    │   │       ├── GitHubApiException.java          # Custom exception
    │   │       └── GlobalExceptionHandler.java      # @ControllerAdvice
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/github/accessreport/
            ├── GitHubAccessReportApplicationTests.java
            ├── service/
            │   └── GitHubServiceImplTest.java
            └── controller/
                └── GitHubControllerTest.java
```



if all not working just run 
git clone https://github.com/12amey/GitHub-orgs-Access-Report.git
cd GitHub-orgs-Access-Report
java "-Dgithub.token=YOUR_TOKEN_HERE" -jar target/github-access-report-1.0.0.jar

