# DotOhTwo Search API

REST API for searching user and reviewable (product, media, etc.) data. It queries Elasticsearch indexes populated by [DotOhTwo-search-server](../DotOhTwo-search-server/) and falls back to the TMDB API when a reviewable search returns too few results.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/search/users?text=&limit=` | Search users by username or display name |
| `GET` | `/search/reviewables?text=&limit=` | Search reviewables; supplements with TMDB when Elasticsearch returns ≤ 5 results |

`limit` defaults to `10` for both endpoints.

---

## Running locally

This service is designed to run as a container on the shared `dotohtwolocalinfra` Docker network managed by [DotOhTwo-local-infra](../DotOhTwo-local-infra/), alongside the Elasticsearch instance that the search-server populates.

### Prerequisites

- Docker with the `dotohtwolocalinfra` network already running (start it from `DotOhTwo-local-infra`)
- A [TMDB API key](https://developer.themoviedb.org/docs/getting-started) (free) for reviewable fallback

### 1. Start the shared infrastructure

```bash
cd ../DotOhTwo-local-infra
docker compose up -d
```

Wait for all services to report `(healthy)`:

```bash
docker compose ps
```

### 2. Start this service

```bash
cd ../DotOhTwo-search-api
TMDB_API_KEY=your_key_here docker compose up -d
```

The API will be available at `http://localhost:8082`.

To stop:

```bash
docker compose down
```

---

## Configuration

All configuration is via environment variables. Defaults are suitable for running inside the `dotohtwolocalinfra` network.

| Variable | Default | Description |
|----------|---------|-------------|
| `ELASTICSEARCH_URIS` | `http://elasticsearch:9200` | Elasticsearch connection URI |
| `TMDB_API_KEY` | _(empty)_ | TMDB API key; fallback is disabled when blank |

---

## Ports

| Service | Host port |
|---------|-----------|
| Search API | `localhost:8082` |
