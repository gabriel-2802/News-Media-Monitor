# News Media Monitor

This project is a news monitoring platform that fetches, classifies, and clusters news articles from various sources. It provides a RESTful API for user interaction and administrative tasks, leveraging custom AI models for text classification and clustering.


## News Processing Pipeline Overview

### RSS Fetching

The application collects news articles by fetching and parsing RSS feeds from registered news sources. This functionality is encapsulated in the `RssFetcher` component.

* **Input:** A list of RSS feed URLs stored in the database.
* **Process:**

  * Performs HTTP GET requests to retrieve RSS XML content.
  * Parses the XML using the Rome library.
  * Extracts key fields including:
    * `title`
    * `link`
    * `description`
    * `pubDate`
    * `content`
* **Output:** A list of raw articles prepared for further classification and storage.

**Example RSS item:**

```xml
<item>
  <title>Breaking News</title>
  <link>https://example.com/news/123</link>
  <description>Something happened...</description>
  <pubDate>Tue, 25 Jun 2025 14:00:00 GMT</pubDate>
</item>
```

Moreover, the `RssFetcher` uses the 'morss.it` api to fetch additional content for articles that are missing the `content` field. This API enriches the article data by providing full text content when available.

## Classification

Classification can be set up in `application.properties` with the following properties:

```properties
monitoring.classification-engine=
```

The allowed values are:
* `custom` - Uses a fine-tuned BERT model for topic classification.
* `hf` - Uses a Hugging Face zero-shot classifier for topic classification.

### Hugging Face Classifier Integration

News articles are semantically classified using a transformer-based zero-shot text classifier.

* **Model:** `valhalla/distilbart-mnli-12-1`
* **Service:** Hugging Face Inference API
* **Component:** `HuggingFaceClassifierEngine`
* **Classification Approach:**

  * The article's `title` and `content` are concatenated into one input string.
  * A candidate label list (topics) is passed to the API.
  * The API returns a ranked list of labels based on textual entailment probabilities.

**Sample API Request:**

```json
{
  "inputs": "Article title and body text",
  "parameters": {
    "candidate_labels": ["Politics", "Technology", "Health"]
  }
}
```

**Sample API Response:**

```json
{
  "labels": ["Politics", "Technology", "Health"],
  "scores": [0.94, 0.03, 0.02]
}
```

The system assigns the label with the highest score to the article.

### Custom BERT Classifier
The custom BERT classifier is based on a fine-tuned `bert-base-uncased`. It is trained to predict high-level topics based on the article's `title` and `content`. The training process involved over 20,000 articles, with the model achieving an accuracy of 85% on a held-out test set.

The model is accesible via 'FastAPI` endpoints.

**Sample API Request:**
```
{
  "text": "Article title and body text"
  "default_topic": " ... "
}
```

**Sample API Response:**

```json
{
  "topic": "Politics",
}
```


## Monitoring Speedup and Efficiency

###  Overview

The application supports configurable parallelism for news monitoring and clustering through an asynchronous task execution strategy. This is controlled by the `monitoring.strategy` property, which can be set to either `async` (enabling multithreaded processing using a thread pool) or `single-threaded` (for sequential execution). When `async` is enabled, the task execution is handled by a configurable `ThreadPoolExecutor`, with the following properties:

```properties
monitoring.async.core-pool-size=6
monitoring.async.max-pool-size=10
monitoring.async.queue-capacity=100
```


### Raw Data

| Threads | Time (s) | Speedup                  | Efficiency (%)              |
| ------- | -------- | ------------------------ | --------------------------- |
| 1       | 45.30    | baseline                 | baseline                    |
| 4       | 30.00    | 45.30 / 30.00 ≈ **1.51** | 1.51 / 4 × 100 ≈ **37.75%** |
| 6       | 22.00    | 45.30 / 22.00 ≈ **2.06** | 2.06 / 6 × 100 ≈ **34.33%** |
| 8       | 27.18    | 45.30 / 27.18 ≈ **1.67** | 1.67 / 8 × 100 ≈ **20.88%** |

---

### Summary Statistics

#### Average Speedup:

$$
\text{Average Speedup} = \frac{1.51 + 2.06 + 1.67}{3} \approx \boxed{1.75}
$$

#### Average Efficiency:

$$
\text{Average Efficiency} = \frac{37.75 + 34.33 + 20.88}{3} \approx \boxed{30.99\%}
$$


### Analysis

* **Best performance** is achieved with **6 threads**, which offers the highest speedup (≈ 2.06×) and reasonable efficiency (≈ 34.33%).
* **8 threads** underperforms due to overhead and likely I/O or CPU contention, leading to decreased efficiency (≈ 20.88%).
* The **average speedup of 1.75×** shows the system benefits significantly from parallelism.
* **Average efficiency of \~31%** indicates that while parallelization is effective, it’s subject to diminishing returns due to factors such as thread management overhead and I/O blocking.



## Search Functionality


The application includes a full-text search system that allows users to search for articles using free-form keywords. It leverages PostgreSQL's native full-text search features, optimized with GIN indexing for performance and scalability.

### Features

* Search across both article titles and content
* Handles case differences and common word forms (e.g., "run", "running")
* Language-aware tokenization and stemming (English)
* High performance, even with large volumes of data

### Overview

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


## Custom AI Services (`ai_models/`)

This module implements the core AI functionality for the News Media Monitor platform. It includes:

- A fine-tuned BERT model for topic classification of news articles
- Article clustering using embeddings and K-Nearest Neighbors (KNN)
- A FastAPI web service that exposes classification and clustering endpoints

The FastAPI service runs independently and is accessed by the main Spring Boot backend application via HTTP. 


### 1. Text Classification

- The classifier is based on a fine-tuned `bert-base-uncased` model using Hugging Face Transformers.
- It is trained to predict high-level topics based on article `title` and `content`.

- The model is loaded using:
  ```python
  AutoTokenizer.from_pretrained(model_path)
  AutoModelForSequenceClassification.from_pretrained(model_path)
  ```

* Input: Raw article text (title + description + content)
* Output: Predicted topic label


### 2. Embeddings and Clustering

* Embeddings are generated using a pretrained SentenceTransformer model.
* Each article is transformed into a dense vector representation.
* Article clustering is performed using FAISS and K-Nearest Neighbors (KNN) on the embedding space.

Key features:
* Batch DB loading to ensure scalability
* Efficient approximate nearest neighbor search using FAISS
* Scalable clustering and similarity detection between articles
* Suitable for comparing large sets of articles over time


### 3. FastAPI Endpoints

The service exposes the following endpoints on `http://localhost:8000`:

| Method | Endpoint    | Description                                 |
| ------ | ----------- | ------------------------------------------- |
| POST   | `/classify` | Classifies an article into a topic          |
| POST   | `/cluster`  | clusters articles from the last 24h and updated the DB       |

---

### Integration Notes

* The Python FastAPI service runs as a container alongside the Spring Boot app.
* The folder `ai_models/` is mounted into the container, enabling live development.

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


#### POST `/api/admin/monitor/start`

Starts the article fetching process from all news sources.

**Responses:**

* **200 OK**

  ```
  Monitor started successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while starting the monitor: {error message}
  ```


#### POST `/api/admin/monitor/cluster`

Starts the clustering process for previously collected articles.

**Responses:**

* **200 OK**

  ```
  Cluster monitor started successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while starting the cluster monitor: {error message}
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

Retrieves the authenticated user's profile.

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

