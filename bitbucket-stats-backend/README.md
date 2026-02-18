# Bitbucket Stats

A full-stack application for tracking developer performance metrics from Bitbucket repositories. Includes a Spring Boot backend API and React dashboard UI.

## Overview

Track and visualize:
- **PR Activity**: Total PRs created, average time to merge, code changes
- **Review Activity**: PRs reviewed, approval rates, comment activity
- **Period Comparisons**: Compare metrics across time periods
- **Team Insights**: Filter by author, repository, date range

## Architecture

```
bitbucket-stats/
├── bitbucket-stats-backend/    # Spring Boot API (Java 17)
├── BitbucketStats-ui/          # React Dashboard (Vite + Tailwind)
└── docker/                     # Docker deployment files
```

## Quick Start

### Option 1: Docker (Recommended)

```bash
cd docker
./setup.sh
```

This builds and runs the backend on port 8080.

Then start the frontend:
```bash
cd ../BitbucketStats-ui
npm install
npm run dev
```

Open http://localhost:5173

### Option 2: Local Development

**Backend:**
```bash
cd bitbucket-stats-backend
./gradlew bootRun
```
Runs on http://localhost:8081

**Frontend:**
```bash
cd BitbucketStats-ui
npm install
npm run dev
```
Runs on http://localhost:5173

## Docker Usage

### Build Image
```bash
cd docker
docker build -f Dockerfile_app -t bitbucket-stats ../
```

### Run Container
```bash
docker run -d --name bitbucket-stats -p 8080:8080 bitbucket-stats
```

### Using setup.sh
The `setup.sh` script automates building and running:
```bash
cd docker
./setup.sh
```

### Stop Container
```bash
docker stop bitbucket-stats
docker rm bitbucket-stats
```

## API Endpoints

### PR Statistics (Author Perspective)
```
GET /pull-requests/stats
```
Returns metrics for PRs created by the authenticated user.

**Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| workspace | Yes | Bitbucket workspace slug |
| repo | Yes | Repository slug (can be repeated for multiple repos) |
| sinceDate | Yes | Start date (YYYY-MM-DD) |
| untilDate | No | End date (YYYY-MM-DD) |
| state | No | PR state: MERGED, OPEN, DECLINED |
| nickname | No | Filter by author nickname |
| includeDiffDetails | No | Include lines added/removed (default: false) |
| includePullRequestDetails | No | Include PR list (default: false) |

**Headers:**
```
username: <bitbucket-username>
appPassword: <bitbucket-app-password>
```

**Example:**
```bash
curl -H "username: myuser" -H "appPassword: myapppassword" \
  "http://localhost:8081/pull-requests/stats?workspace=myworkspace&repo=myrepo&sinceDate=2025-01-01&includeDiffDetails=true"
```

**Response:**
```json
{
  "period": "FROM: 2025-01-01 TO: 2025-12-31",
  "total_pull_requests": 47,
  "avg_time_open_hours": 111,
  "avg_comment_count": 5,
  "avg_files_changed": 20,
  "avg_lines_added": 363,
  "avg_lines_removed": 122,
  "pull_request_details": [...]
}
```

### Review Statistics (Reviewer Perspective)
```
GET /pull-requests/reviews/stats
```
Returns metrics for PRs reviewed by the authenticated user.

**Response:**
```json
{
  "period": "FROM: 2025-01-01 TO: 2025-12-31",
  "total_pull_requests_approved": 116,
  "total_pull_requests_reviewed": 463,
  "total_comments": 234,
  "approved_percentage": 25.05,
  "commented_percentage": 45.2
}
```

## Bitbucket App Password Setup

1. Go to https://bitbucket.org/account/settings/app-passwords/
2. Click "Create app password"
3. Select permissions:
   - **Repositories**: Read
   - **Pull Requests**: Read
4. Copy the generated password

## Frontend Features

See [BitbucketStats-ui/README.md](../BitbucketStats-ui/README.md) for detailed frontend documentation.

Key features:
- Period comparison with trend indicators
- Stale PR filtering (exclude PRs open > X days)
- Business hours calculation (exclude weekends)
- CSV export for reporting
- Persistent settings (localStorage)

## Configuration

### Backend (application.yml)
```yaml
server:
  port: 8081

spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

### Frontend API URL
Edit `src/api/bitbucketApi.js`:
```javascript
const API_BASE = 'http://localhost:8081'
```

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 3.x (WebFlux)
- Gradle

**Frontend:**
- React 18
- Vite
- Tailwind CSS
- React Query
- Lucide Icons

## Troubleshooting

### 401 Unauthorized
- Verify app password (not your Bitbucket password)
- Check app password has correct scopes
- Verify workspace/repo names are correct

### 500 / Retries Exhausted
- Bitbucket API rate limiting - wait 1-2 minutes
- Reduce date range to fetch fewer PRs

### CORS Errors
- Backend includes CORS config for localhost:5173/5174
- Check `WebConfig.java` if using different port

## License

MIT
