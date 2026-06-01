# Flight Delay Analyzer

A Java/Spring Boot REST API that ingests historical BTS On-Time Performance data and predicts flight delay risk for any domestic US route, airline, and travel month.

![CI](https://github.com/YOUR_USERNAME/flight-delay-analyzer/actions/workflows/ci.yml/badge.svg)

---

## The Problem

Flight delays cost US airlines $33 billion annually and passengers billions more in missed connections and rebooking fees. Existing tools tell you *current* delays — this API predicts *expected* delay risk using years of historical performance data, so you can make smarter booking decisions before you buy a ticket.

## How It Works

The API ingests raw CSV data from the **Bureau of Transportation Statistics** (BTS) On-Time Performance dataset — the same data source used by researchers and aviation analysts. For any origin/destination/carrier/month combination, it:

1. Retrieves historical flight records for that query
2. Computes a **composite risk score** (0–100) weighted across three signals:
   - **Delay rate** (40%) — % of flights arriving >15 min late
   - **Average delay severity** (35%) — mean arrival delay in minutes
   - **Cancellation rate** (25%) — % of flights cancelled
3. Returns a structured JSON response with the score, label, and human-readable insight

The prediction logic is fully transparent and explainable — no black box, every factor is documented in code.

## API Endpoints

### `GET /api/v1/delay-risk`
Predict delay risk for a specific route.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `origin`  | ✓ | Airport code (e.g. `ORD`) |
| `dest`    | ✓ | Airport code (e.g. `LAX`) |
| `carrier` | — | Airline code (e.g. `AA`, `UA`, `DL`) |
| `month`   | — | Travel month 1–12 |

```bash
curl "http://localhost:8080/api/v1/delay-risk?origin=ORD&dest=LAX&carrier=AA&month=12"
```

```json
{
  "origin": "ORD",
  "dest": "LAX",
  "carrier": "AA",
  "month": 12,
  "sampleSize": 847,
  "avgArrivalDelayMinutes": 14.3,
  "delayRatePercent": 32.1,
  "cancellationRatePercent": 2.4,
  "riskScore": 41.7,
  "riskLabel": "MODERATE",
  "insight": "ORD→LAX on AA in month 12: MODERATE risk. 32.1% of flights arrive >15 min late, avg delay 14.3 min, 2.4% cancellation rate. Based on 847 historical flights."
}
```

### `GET /api/v1/worst-routes`
Leaderboard of routes with highest average arrival delay.

```bash
curl "http://localhost:8080/api/v1/worst-routes?limit=10&minFlights=200"
```

### `GET /api/v1/airline-stats`
All airlines ranked by delay performance with a EXCELLENT / GOOD / AVERAGE / POOR label.

```bash
curl "http://localhost:8080/api/v1/airline-stats"
```

### `GET /api/v1/worst-airports`
Airports with worst average departure delays.

```bash
curl "http://localhost:8080/api/v1/worst-airports?limit=15"
```

### `GET /api/v1/stats`
Dataset-level summary: total flights, routes, airlines, overall delay rate.

```bash
curl "http://localhost:8080/api/v1/stats"
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | H2 (in-memory dev) / PostgreSQL (prod) |
| CSV parsing | OpenCSV |
| Build | Maven |
| Containerization | Docker + docker-compose |
| CI/CD | GitHub Actions |

## Getting Started

### 1. Get the data

Download from the [BTS On-Time Performance portal](https://www.transtats.bts.gov/DL_SelectFields.aspx?gnoyr_VQ=FGK).

Select at minimum: `YEAR`, `MONTH`, `DAY_OF_WEEK`, `OP_UNIQUE_CARRIER`, `ORIGIN`, `DEST`, `DEP_DELAY`, `ARR_DELAY`, `CANCELLED`, `CARRIER_DELAY`, `WEATHER_DELAY`, `NAS_DELAY`, `SECURITY_DELAY`, `LATE_AIRCRAFT_DELAY`.

Save the file to `src/main/resources/bts_flights.csv`.

### 2. Run locally

```bash
./mvnw spring-boot:run
```

### 3. Run with Docker

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

### 4. Run tests

```bash
./mvnw test
```

## Project Structure

```
src/
├── main/java/com/flightdelay/
│   ├── controller/        # REST endpoints
│   ├── service/           # Business logic + prediction
│   ├── repository/        # JPA queries
│   ├── model/             # Flight entity
│   ├── dto/               # Response records
│   └── ingestion/         # BTS CSV loader
└── test/java/com/flightdelay/
    └── service/           # Unit tests
```

## Design Decisions

**Why rule-based prediction instead of ML?**
The BTS dataset has ~5M+ rows per year. A weighted composite of historical rates is highly accurate for this use case, fully explainable, and doesn't require a model retraining pipeline. The architecture is designed to swap in an ML layer (e.g. a scikit-learn model served via a sidecar) without changing the API contract.

**Why H2 by default?**
Zero-setup dev experience. Switch to Postgres for production by overriding `SPRING_DATASOURCE_*` environment variables — no code changes needed.

**Why progressive query fallback?**
Route + carrier + month gives the most specific prediction. When sample size is too small, the service automatically falls back to route + carrier, then route only. This balances specificity with statistical reliability.

## Data Source

Bureau of Transportation Statistics, On-Time Performance dataset.
https://www.transtats.bts.gov
