# News Media Monitor

admin user: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJnYWJyaWVsIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzUwMzIxNjM1LCJleHAiOjE3NTA5MjY0MzV9.3jQ5NvRvX4Rx0Pvx_9Pdo5VHWKW7IexhlJIZVhFe5BtuucFsm1Fp8ZDJOI5XTICZvJ8KWMBY4-oUG-CJ0-li1Q

Understood. Here’s the cleaned-up `.md` documentation **without emojis**, with full request/response examples where applicable:


### Base URL: `/api/auth`

---

### POST `/register`

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

### POST `/login`

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

### Base URL: `/api/feed`

---

### GET `/topics`

Returns all available topics.

**Response:**

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

---

### Base URL: `/api/admin`

Requires admin authentication.

---

### GET `/users`

Returns all users.

**Response:**

* **200 OK**

  ```json
  [
    {
      "username": "john_doe",
      "email": "john@example.com",
      "roles": ["USER"]
    },
    {
      "username": "admin",
      "email": "admin@example.com",
      "roles": ["ADMIN"]
    }
  ]
  ```

---

### GET `/users/{username}`

Returns user details by username.

**Path Parameter:**

* `username` (String)

**Response:**

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

---

### DELETE `/users/{username}`

Deletes a user by username.

**Path Parameter:**

* `username` (String)

**Response:**

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
  An error occurred while deleting the user: {error message}
  ```

---

### POST `/topic/{topicName}`

Creates a new topic.

**Path Parameter:**

* `topicName` (String)

**Response:**

* **200 OK**

  ```
  Topic created successfully
  ```

* **409 Conflict**

  ```
  Topic already exists
  ```

---

### DELETE `/topic/{topicName}`

Deletes a topic.

**Path Parameter:**

* `topicName` (String)

**Response:**

* **200 OK**

  ```
  Topic deleted successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while deleting the topic: {error message}
  ```

---

### POST `/news_source`

Creates a new news source.

**Request Body:**

```json
{
  "name": "BBC",
  "baseUrl": "https://www.bbc.com",
  "rssUrl": "https://feeds.bbci.co.uk/news/rss.xml"
}
```

**Response:**

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

---

### DELETE `/news_source/{id}`

Deletes a news source.

**Path Parameter:**

* `id` (Long)

**Response:**

* **200 OK**

  ```
  News source deleted successfully
  ```

* **404 Not Found**

  ```
  Invalid or non-existent ID
  ```

* **500 Internal Server Error**

  ```
  An error occurred while deleting the news source: {error message}
  ```

---

### GET `/news_sources`

Returns all registered news sources.

**Response:**

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

---

### POST `/monitor/start`

Manually starts the monitoring process.

**Response:**

* **200 OK**

  ```
  Monitor started successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while starting the monitor: {error message}
  ```

---