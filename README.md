# Metrics Workshop

A complete Docker Compose setup for learning Prometheus monitoring with a Spring Boot application.

## Quick Start

```bash
docker compose up --build
```

## Services

| Service       | URL                          | Description                    |
|---------------|------------------------------|--------------------------------|
| Spring App    | http://localhost:8080        | Sample application             |
| Prometheus    | http://localhost:9090        | Metrics & alerting             |
| Grafana       | http://localhost:3000        | Dashboards (admin/admin)       |
| Alertmanager  | http://localhost:9093        | Alert management               |
| Elasticsearch | http://localhost:9200        | Log storage                    |
| Kibana        | http://localhost:5601        | Log visualization              |

## Application Endpoints

| Endpoint           | Method | Description                              |
|--------------------|--------|------------------------------------------|
| `/fast`            | GET    | Returns immediately (200)                |
| `/slow`            | GET    | Returns after 300-500ms delay (200)      |
| `/error`           | GET    | Returns 500 randomly (~30%)              |
| `/enable-slow`     | POST   | Enable global slow mode                  |
| `/enable-errors`   | POST   | Enable global error mode (higher rate)   |
| `/disable-modes`   | POST   | Disable all artificial modes             |
| `/actuator/prometheus` | GET | Prometheus metrics endpoint          |

## Custom Metrics

- `workshop_requests_total` - Counter with endpoint/status labels
- `workshop_errors_total` - Counter for 5xx responses
- `workshop_queue_size` - Gauge (simulated queue)
- `workshop_active_requests` - Gauge (concurrent requests)
- `workshop_request_duration_seconds` - Histogram (request latency)

## Useful PromQL Queries

### Request Rate
```promql
rate(workshop_requests_total[1m])
```

### Error Rate Percentage
```promql
sum(rate(workshop_requests_total{status=~"5.."}[1m])) / sum(rate(workshop_requests_total[1m])) * 100
```

### P95 Latency
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))
```

### Active Requests
```promql
workshop_active_requests
```

## Simulating Incidents

### Enable High Latency
```bash
curl -X POST http://localhost:8080/enable-slow
```

### Enable High Error Rate
```bash
curl -X POST http://localhost:8080/enable-errors
```

### Return to Normal
```bash
curl -X POST http://localhost:8080/disable-modes
```

## Alerts

- **AppDown** - Application is unreachable
- **HighErrorRate** - Error rate > 5% for 1 minute
- **HighLatencyP95** - P95 latency > 500ms for 1 minute

## Elasticsearch Logs

### View app logs
```bash
curl -s "http://localhost:9200/app-logs-*/_search?pretty&size=10"
```

### Search for errors
```bash
curl -s "http://localhost:9200/app-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": { "match": { "message": "ERROR" } },
  "size": 10
}'
```

## Stop Services

```bash
docker compose down
```

## Clean Up (including volumes)

```bash
docker compose down -v
```

