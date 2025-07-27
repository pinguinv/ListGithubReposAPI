# ListGithubReposAPI

API for listing GitHub user repositories, excluding forks, with branch names and latest commit SHAs.

## Overview

This project provides a REST API to fetch all non-fork repositories for a specified GitHub user, including repository
names, owners, and branch details.

## Prerequisites

- Java 21
- Gradle 3.5.4
- A valid GitHub Personal Access Token (PAT) with `repo` scope

## Setup

1. **Clone the Repository**

   ```bash
   git clone https://github.com/pinguinv/ListGithubReposAPI.git
   cd ListGithubReposAPI
   ```

2. **Configure GitHub Personal Access Token**

    - Generate a fine-grained Personal Access Token as described in
      the [GitHub documentation](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-fine-grained-personal-access-token).
    - Update the `github.access.token` property in the `src/main/resources/application.properties` file with your token:

      ```properties
      github.access.token=your_personal_access_token
      ```

3. **Build the Project**

   ```bash
   gradle build
   ```

4. **Run the Application**

   ```bash
   gradle bootRun
   ```

   The API will be available at `http://localhost:8080`.

## API Usage

### Endpoint: List User Repositories

- **Method**: `GET`
- **Path**: `/getGithubUserRepos/{user}`
- **Description**: Fetches non-fork repositories for the specified GitHub user.
- **Path Parameter**:
    - `user`: The GitHub username (e.g., `octocat`).

#### Example Request

```bash
curl http://localhost:8080/getGithubUserRepos/octocat
```

#### Success Response

- **Status Code**: `200 OK`
- **Content-Type**: `application/json`
- **Body**:

  ```json
  [
    {
      "repositoryName": "hello-world",
      "ownerLogin": "octocat",
      "branches": [
        {
          "name": "main",
          "lastCommitSha": "7fd1a60b01f91b314f59955a4e4d4e80c7d8b6b"
        },
        {
          "name": "develop",
          "lastCommitSha": "9c988a9b6c34e3b8f16d76b6e8f6a6b7e6b7e6b"
        }
      ]
    }
  ]
  ```

#### Error Response (Non-existing User)

- **Status Code**: `404 Not Found`
- **Content-Type**: `application/json`
- **Body**:

  ```json
  {
    "status": "404",
    "message": "User not found"
  }
  ```

## Integration Tests

Integration tests are not implemented yet.