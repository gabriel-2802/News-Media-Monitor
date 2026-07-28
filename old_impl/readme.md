# News Media Monitor

A distributed news monitoring platform that automatically fetches, classifies, and clusters news articles from RSS feeds. The system uses custom AI models for intelligent topic classification and article clustering, with a RESTful API for user interaction.

## Components

### Spring App (Backend API)

**Location:** `demo/`

The main backend service providing REST APIs for users and administrators.

**Responsibilities:**
- User authentication and authorization (JWT-based)
- Article feed management and search
- News source administration
- User profile and notification management
- Scheduled job creation for RSS monitoring

**Key Features:**
- Role-based access control (ROLE_USER, ROLE_ADMIN)
- Full-text article search by keyword, topic, or source
- Topic-based article filtering
- Article clustering views

**Configuration:**
| Property | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://db:5432/news_monitor_db` |
| `AI_SERVICE_URL` | AI service base URL | `http://ai_service:8000` |

**Port:** 8080

---

### RSS Worker

**Location:** `rss/`

Distributed worker service responsible for fetching articles from RSS feeds. Runs as multiple replicas (default: 3) for horizontal scaling. Built with Spring Boot.

**Responsibilities:**
- Periodic RSS feed fetching (configurable interval, default: 3 hours)
- Article parsing and extraction
- Topic classification via AI Service
- Coordinated clustering trigger across workers
- Duplicate detection (based on unique article URLs)

**Worker Coordination:**
- Uses `FOR UPDATE SKIP LOCKED` for distributed locking
- Each worker claims news sources atomically to prevent duplicate processing
- After all sources are processed, exactly one worker triggers clustering
- Automatic retry with configurable max consecutive failures

**Communication:**
- **AI Service** → HTTP POST to `/classify` for topic prediction
- **AI Service** → HTTP POST to `/cluster` to trigger article clustering
- **Database** → JDBC for article storage and job coordination

**Configuration:**
| Property | Description | Default |
|----------|-------------|---------|
| `CLASSIFICATION_API_URL` | Classification endpoint | `http://ai_service:8000/classify` |
| `CLUSTERING_API_URL` | Clustering endpoint | `http://ai_service:8000/cluster` |
| `worker.fetch-interval-ms` | Interval between fetch cycles | `10800000` (3 hours) |
| `WORKER_INDEX` | Worker instance identifier | Auto-assigned via Docker |
| `WORKER_COUNT` | Total number of workers | `3` |

**Port:** 8081 (metrics only, no HTTP API)

---

### AI Service

**Location:** `ai_models/`

Python-based microservice providing ML capabilities for article classification and clustering. Built with FastAPI.

**Responsibilities:**
- **Topic Classification:** Predicts article topics using a fine-tuned DistilBERT model (86% accuracy)
- **Article Clustering:** Groups similar articles using embeddings and FAISS indexing

**ML Models:**
| Model | Purpose | Technology |
|-------|---------|------------|
| NewsClassifier | Topic prediction | DistilBERT (fine-tuned), ~40 topic categories |
| TextEmbedder | Semantic similarity | sentence-transformers/all-MiniLM-L6-v2 |
| Clustering | Article grouping | FAISS (IndexFlatIP with cosine similarity) |

**API Endpoints:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/classify` | POST | Classify article text into a topic |
| `/cluster` | POST | Trigger clustering of unprocessed articles |
| `/metrics` | GET | Prometheus metrics |

**Classification Request:**
```json
{
  "text": "Article title and summary...",
  "default_topic": "politics"
}
```

**Classification Response:**
```json
{
  "topic": "politics"
}
```

**Clustering Algorithm:**
1. Fetch unclustered articles from database
2. Generate embeddings using SentenceTransformer
3. Build FAISS index with cosine similarity
4. Apply graph-based clustering with configurable threshold
5. Persist cluster assignments to database

**Configuration:**
| Property | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `db` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_USER` | Database user | `admin` |
| `DB_PASSWORD` | Database password | `admin` |

**Port:** 8000 (exposed as 8002 externally)

---

### PostgreSQL Database

**Location:** `db/`

Central data store for all application data.

**Key Tables:**
| Table | Description |
|-------|-------------|
| `articles` | News articles with content, metadata, topic, and cluster assignments |
| `article_clusters` | Groups of semantically similar articles |
| `news_sources` | RSS feed URLs and fetch state |
| `topics` | Predefined topic categories (~40 categories) |
| `users` | User accounts |
| `roles` | Role definitions (ROLE_USER, ROLE_ADMIN) |
| `notifications` | User notifications |

**Initialization:** The `db/init.sql` script seeds roles and predefined topics on first startup.

**Port:** 5432

---

### Monitoring Stack

**Prometheus** and **Grafana** provide observability for the platform.

**Prometheus Configuration:**
- Scrapes metrics from all services every 10 seconds
- Uses DNS service discovery for Docker Swarm tasks

**Scraped Endpoints:**
| Service | Endpoint | Port |
|---------|----------|------|
| Spring App | `/actuator/prometheus` | 8080 |
| RSS Worker | `/actuator/prometheus` | 8081 |
| AI Service | `/metrics` | 8000 |

**Key Metrics:**
- `http_requests_total` - HTTP request counts by method, path, status
- `http_request_duration_seconds` - Request latency histograms
- `ai_classify_total` - Classification call counts (success/fallback)
- `ai_cluster_total` - Clustering call counts (success/error)
- Spring Actuator JVM/HTTP metrics

**Grafana:** `http://localhost:3000` (admin/admin)  
**Prometheus:** `http://localhost:9090`

---

## Communication Flow

### Article Ingestion Pipeline

```
1. RSS Worker (Scheduled)
   │
   ├── Claims next available NewsSource (FOR UPDATE SKIP LOCKED)
   │
   ├── Fetches RSS feed
   │
   ├── For each article:
   │   │
   │   └── POST /classify → AI Service
   │       │
   │       └── Returns predicted topic
   │
   ├── Batch insert articles to PostgreSQL
   │
   ├── NOTIFY new_articles_ready → PostgreSQL
   │   │
   │   └── Spring App (LISTEN) receives notification
   │       │
   │       └── Can trigger real-time updates/notifications
   │
   └── When all sources processed:
       │
       └── POST /cluster → AI Service
           │
           ├── Fetches unclustered articles from DB
           ├── Generates embeddings
           ├── Clusters using FAISS
           └── Updates article cluster_id in DB
```


### Inter-Service Communication

| Source | Target | Protocol | Endpoint | Purpose |
|--------|--------|----------|----------|---------|
| RSS Worker | AI Service | HTTP | POST `/classify` | Topic classification |
| RSS Worker | AI Service | HTTP | POST `/cluster` | Trigger clustering |
| RSS Worker | PostgreSQL | JDBC | - | Article storage, job coordination |
| RSS Worker | Spring App | PostgreSQL NOTIFY | `new_articles_ready` | Signal new articles available |
| AI Service | PostgreSQL | psycopg2 | - | Fetch articles for clustering |
| Spring App | PostgreSQL | JDBC | - | All CRUD operations |

### PostgreSQL NOTIFY/LISTEN

The system uses PostgreSQL's built-in pub/sub mechanism for real-time inter-service communication

This allows the Spring App to react immediately when new articles are inserted, enabling real-time features like push notifications to users without polling.


### Service URLs

| Service | URL |
|---------|-----|
| REST API | http://localhost:8080 |
| AI Service | http://localhost:8002 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

---

## API Documentation

### Authentication

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register new user |
| `/api/auth/login` | POST | Login and get JWT token |

### Feed Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/feed/topics` | GET | User | List all topics |
| `/api/feed/sources` | GET | User | List all news sources |
| `/api/feed/articles` | GET | User | List all articles |
| `/api/feed/articles/{topic}` | GET | User | Articles by topic |
| `/api/feed/search` | POST | User | Search articles |

### Admin Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/admin/users` | GET | Admin | List all users |
| `/api/admin/sources` | POST | Admin | Add news source |
| `/api/admin/sources/{id}` | DELETE | Admin | Remove news source |

