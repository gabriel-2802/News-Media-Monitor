# News Media Monitor

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


## Monitoring Speedup and Efficiency

The article gathering process was parallelized using a Custom ThreadPool Executor to improve the system's throughput when collecting news from multiple sources.
Moreover, the classifying process was also parallelized using parallel streams to enhance the overall performance of the application.

### Speedup Achieved (average results using 4-6-8 threads)
$$
\text{Speedup} = \frac{T_{\text{average\_serial}}}{T_{\text{average\_parallel}}} \approx 2.67

$$



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

Here's the technical section in **Markdown format (`README.md`)** without emojis, suitable for placing inside your `ai_models/` folder:


### Custom AI Services (`ai_models/`)

This module implements the core AI functionality for the News Media Monitor platform. It includes:

- A fine-tuned BERT model for topic classification of news articles
- Article clustering using embeddings and K-Nearest Neighbors (KNN)
- A FastAPI web service that exposes classification and clustering endpoints

The FastAPI service runs independently and is accessed by the main Spring Boot backend application via HTTP.

---

#### 1. Text Classification

- The classifier is based on a fine-tuned `bert-base-uncased` model using Hugging Face Transformers.
- It is trained to predict high-level topics based on article `title` and `content`.

- The model is loaded using:
  ```python
  AutoTokenizer.from_pretrained(model_path)
  AutoModelForSequenceClassification.from_pretrained(model_path)
  ```

* Input: Raw article text (title + description + content)
* Output: Predicted topic label

---

#### 2. Embeddings and Clustering

* Embeddings are generated using a pretrained SentenceTransformer model.
* Each article is transformed into a dense vector representation.
* Article clustering is performed using FAISS and K-Nearest Neighbors (KNN) on the embedding space.

Key features:
* Batch DB loading to ensure scalability
* Efficient approximate nearest neighbor search using FAISS
* Scalable clustering and similarity detection between articles
* Suitable for comparing large sets of articles over time

---

#### 3. FastAPI Endpoints

The service exposes the following endpoints on `http://localhost:8000`:

| Method | Endpoint    | Description                                 |
| ------ | ----------- | ------------------------------------------- |
| POST   | `/classify` | Classifies an article into a topic          |
| POST   | `/cluster`  | clusters articles from the last 24h and updated the DB       |

---

#### Integration Notes

* The Python FastAPI service runs as a container alongside the Spring Boot app.
* The folder `ai_models/` is mounted into the container, enabling live development.


---

### API Documentation

### POST `/api/auth/register`

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

### POST `/api/auth/login`

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

### GET `/api/feed/topics`

Returns all available topics.

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

---

### GET `/api/admin/users`

Returns all users.

**Responses:**

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

### GET `/api/admin/users/{username}`

Returns user details by username.

**Path Parameter:**

* `username` (String)

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

---

### DELETE `/api/admin/users/{username}`

Deletes a user by username.

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
  An error occurred while deleting the user: {error message}
  ```

---

### POST `/api/admin/topic/{topicName}`

Creates a new topic.

**Path Parameter:**

* `topicName` (String)

**Responses:**

* **200 OK**

  ```
  Topic created successfully
  ```

* **409 Conflict**

  ```
  Topic already exists
  ```

---

### DELETE `/api/admin/topic/{topicName}`

Deletes a topic.

**Path Parameter:**

* `topicName` (String)

**Responses:**

* **200 OK**

  ```
  Topic deleted successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while deleting the topic: {error message}
  ```

---

### POST `/api/admin/news_source`

Creates a new news source.

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

---

### DELETE `/api/admin/news_source/{id}`

Deletes a news source.

**Path Parameter:**

* `id` (Long)

**Responses:**

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

### GET `/api/admin/news_sources`

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

---

### POST `/api/admin/monitor/start`

Manually starts the monitoring process.

**Responses:**

* **200 OK**

  ```
  Monitor started successfully
  ```

* **500 Internal Server Error**

  ```
  An error occurred while starting the monitor: {error message}
  ```

