# News Media Monitor

This project is a news monitoring platform that fetches, classifies, and clusters news articles from various sources. It provides a RESTful API for user interaction and administrative tasks, leveraging custom AI models for text classification and clustering as well as worker containers for background processing.

## API Documentation

### Authentication Endpoints

#### POST `/api/auth/register`

Registers a new user.

**Request Body:**

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

**Responses:**

* **200 OK**

  ```
  User registered successfully
  ```

* **400 Bad Request**

  ```
  Email already exists
  ```

* **409 Conflict**

  ```
  Username already exists
  ```

* **500 Internal Server Error**

  ```
  An error occurred during registration : {error message}
  ```

---

#### POST `/api/auth/login`

Authenticates a user and returns an authentication token.

**Request Body:**

```json
{
  "username": "john_doe",
  "password": "SecurePass123"
}
```

**Responses:**

* **200 OK**

  ```json
  {
    "token": "jwt-token-example",
    "message": "Login successful"
  }
  ```

* **400 Bad Request**

  ```json
  {
    "message": "Invalid credentials"
  }
  ```

* **500 Internal Server Error**

  ```json
  {
    "message": "An error occurred: {error message}"
  }
  ```

---

### ADMIN Endpoints

Requires `ADMIN` role for access.


#### GET `/api/admin/users`

Retrieves all registered users.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "username": "admin",
      "email": "admin@example.com",
      "roles": ["ADMIN"]
    },
    {
      "username": "john_doe",
      "email": "john@example.com",
      "roles": ["USER"]
    }
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```

#### GET `/api/admin/users/{username}`

Fetches information for a specific user.

**Path Parameter:**

* `username` (String): The username to look up.

**Responses:**

* **200 OK**

  ```json
  {
    "username": "john_doe",
    "email": "john@example.com",
    "roles": ["USER"]
  }
  ```

* **404 Not Found**

  ```json
  null
  ```

* **500 Internal Server Error**

  ```json
  null
  ```


#### DELETE `/api/admin/users/{username}`

Deletes a user from the system.

**Path Parameter:**

* `username` (String)

**Responses:**

* **200 OK**

  ```
  User deleted successfully
  ```

* **404 Not Found**

  ```
  User not found
  ```

* **500 Internal Server Error**

  ```
  An error occurred while deleting the user {username}
  ```

#### POST `/api/admin/news_source`

Adds a new news source for monitoring.

**Request Body:**

```json
{
  "name": "BBC",
  "baseUrl": "https://www.bbc.com",
  "rssUrl": "https://feeds.bbci.co.uk/news/rss.xml"
}
```

**Responses:**

* **200 OK**

  ```json
  {
    "id": 4,
    "name": "BBC",
    "baseUrl": "https://www.bbc.com",
    "rssUrl": "https://feeds.bbci.co.uk/news/rss.xml"
  }
  ```

* **409 Conflict**

  ```json
  null
  ```

* **500 Internal Server Error**

  ```json
  null
  ```


#### DELETE `/api/admin/news_source/{id}`

Deletes a news source by its ID.

**Path Parameter:**

* `id` (Long)

**Responses:**

* **200 OK**

  ```json
  {
    "id": 4,
    "name": "BBC",
    "baseUrl": "https://www.bbc.com",
    "rssUrl": "https://feeds.bbci.co.uk/news/rss.xml"
  }
  ```

* **404 Not Found**

  ```json
  null
  ```

* **500 Internal Server Error**

  ```json
  null
  ```


#### DELETE `/api/admin/purge_all`

Deletes all stored articles in the system.

**Responses:**

* **200 OK**

  ```
  All articles deleted successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while deleting all articles: {error message}
  ```

---
### Feed Endpoints

Requires no authentication for access.

#### GET `/api/feed/topics`

Retrieves a list of all available topics.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 1,
      "name": "Politics"
    },
    {
      "id": 2,
      "name": "Technology"
    }
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```



#### GET `/api/feed/news_sources`

Returns all registered news sources.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 1,
      "name": "BBC",
      "baseUrl": "https://www.bbc.com",
      "rssUrl": "https://feeds.bbci.co.uk/news/rss.xml"
    },
    {
      "id": 2,
      "name": "CNN",
      "baseUrl": "https://www.cnn.com",
      "rssUrl": "https://rss.cnn.com/rss/edition.rss"
    }
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```



#### GET `/api/feed/articles`

Returns all available articles.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 101,
      "title": "Economic Forecast Released",
      "content": "Details about economic report...",
      "topicName": "Economy",
      "sourceName": "Reuters"
    },
    ...
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```


#### GET `/api/feed/articles/{topicName}`

Returns articles filtered by topic name.

**Path Parameter:**

* `topicName` (String): Name of the topic.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 202,
      "title": "New Tech Innovations",
      "content": "Highlights from tech summit...",
      "topicName": "Technology",
      "sourceName": "Wired"
    }
  ]
  ```

* **404 Not Found**

  ```json
  null
  ```

* **500 Internal Server Error**

  ```json
  null
  ```



#### GET `/api/feed/articles/by_cluster/{clusterId}`

Retrieves all articles that belong to a specific cluster.

**Path Parameter:**

* `clusterId` (Long): Cluster identifier.

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 301,
      "title": "Climate Policy Agreement Reached",
      "clusterId": 5,
      "topicName": "Environment"
    },
    ...
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```


#### POST `/api/feed/search`

Searches for articles based on full-text input.

**Request Body:**

```json
{
  "query": "energy crisis",
  "filters": {
    "topics": ["Politics", "Environment"],
    "sources": ["BBC"]
  }
}
```

**Responses:**

* **200 OK**

  ```json
  [
    {
      "id": 400,
      "title": "Energy Crisis Deepens",
      "content": "Europe faces new energy concerns...",
      "topicName": "Politics",
      "sourceName": "BBC"
    }
  ]
  ```

* **500 Internal Server Error**

  ```json
  null
  ```

--- 
### User Endpoints


Requires authentication via JWT for all endpoints.

#### GET `/api/user/profile/{username}`

Retrieves the authenticated user's profile and notifications.

**Path Parameter:**

* `username` (String): Must match the currently authenticated user.

**Authentication:** Required (JWT)

**Responses:**

* **200 OK**

  ```json
  {
    "username": "john_doe",
    "email": "john@example.com",
    "roles": ["USER"]
  }
  ```

* **403 Forbidden**

  ```
  null
  ```

* **404 Not Found**

  ```
  null
  ```

* **500 Internal Server Error**

  ```
  null
  ```



#### POST `/api/user/subscribe/{topicId}`

Subscribes the authenticated user to a specific topic.

**Path Parameter:**

* `topicId` (Long): ID of the topic to subscribe to.

**Authentication:** Required (JWT)

**Responses:**

* **200 OK**

  ```json
  {
    "id": 3,
    "name": "Technology"
  }
  ```

* **404 Not Found**

  ```
  null
  ```

* **409 Conflict**

  ```
  null
  ```

* **500 Internal Server Error**

  ```
  null
  ```

#### DELETE `/api/user/unsubscribe/{topicId}`

Unsubscribes the authenticated user from a specific topic.

**Path Parameter:**

* `topicId` (Long): ID of the topic to unsubscribe from.

**Authentication:** Required (JWT)

**Responses:**

* **204 No Content**

* **404 Not Found**

* **500 Internal Server Error**

---

### Monitoring Activity Endpoints

#### GET `http://localhost:9090`

Prometheus metrics endpoint for monitoring application performance and health, accessible without authentication.

####  GET `http://localhost:8080/actuator/health`

Spring Boot Actuator health check endpoint to verify application status, accessible without authentication.

