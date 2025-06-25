# News Media Monitor

## Monitoring Speedup and Efficiency

The article gathering process was parallelized using a Custom ThreadPool Executor to improve the system's throughput when collecting news from multiple sources.
Moreover, the classifying process was also parallelized using parallel streams to enhance the overall performance of the application.

### Speedup Achieved
$$
\text{Speedup} = \frac{T_\text{serial}}{T_\text{parallel}}\approx 2.67
$$

### Efficiency (using 5 threads)

$$
\text{Efficiency} = \frac{\text{Speedup}}{\text{Number of threads}} = \frac{2.67}{5} \approx 0.534 = 53.4\%
$$

---

## Search Functionality

The application includes a full-text search system that allows users to search for articles using free-form keywords. It leverages PostgreSQL's native full-text search features, optimized with GIN indexing for performance and scalability.

### Features

* Search across both article titles and content
* Handles case differences and common word forms (e.g., "run", "running")
* Language-aware tokenization and stemming (English)
* High performance, even with large volumes of data

### Technical Overview

PostgreSQL provides full-text search support through `tsvector` (searchable document format) and `tsquery` (user query format). These are used together to match articles efficiently.

#### `to_tsvector(language, text)`

Transforms a block of text into a normalized form by:

* Removing stopwords (e.g., "the", "and", "în", "la")
* Tokenizing words
* Applying stemming (e.g., "economia" becomes "econom")

#### `plainto_tsquery(language, search)`

Processes user input into a `tsquery`, applying the same language rules as `to_tsvector`.

#### `@@` Operator

Evaluates whether the text vector matches the search query:

```sql
to_tsvector(...) @@ plainto_tsquery(...)
```

### Example SQL Query

```sql
SELECT * FROM articles
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'energy crisis');
```

This query returns all articles whose title or content includes both "energy" and "crisis" in any form or order.

### Indexing for Performance

To avoid full table scans during searches, a GIN (Generalized Inverted Index) is used:

```sql
CREATE INDEX idx_articles_fts
ON articles
USING gin(to_tsvector('english', title || ' ' || content));
```

This index maps each word in the dataset to the rows where it appears, enabling fast search even over large text fields.

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