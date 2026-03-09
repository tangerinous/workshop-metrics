# 📊 Monitoring Workshop

Учебный стенд для изучения мониторинга приложений. Включает сбор метрик, визуализацию, алертинг и централизованное логирование.

---

## ✅ Требования

- Docker и Docker Compose (или Docker Desktop)
- Git
- Свободные порты: `8080`, `9090`, `3000`, `9093`, `9200`, `5601`

---

## 🚀 Запуск

```bash
git clone https://github.com/tangerinous/workshop-metrics.git
cd metrics-workshop
docker compose up --build
```

Дождитесь запуска всех контейнеров (1–2 минуты).

---

## 🔗 Сервисы

| Сервис | URL | Описание |
|--------|-----|----------|
| Application | http://localhost:8080 | Spring Boot приложение |
| Prometheus | http://localhost:9090 | Метрики и алерты |
| Grafana | http://localhost:3000 | Дашборды (логин: `admin` / `admin`) |
| Alertmanager | http://localhost:9093 | Управление алертами |
| Kibana | http://localhost:5601 | Просмотр логов |
| Elasticsearch | http://localhost:9200 | Хранение логов |

---

## 🛑 Остановка

```bash
docker compose down
```

Для удаления данных (volumes):

```bash
docker compose down -v
```

---

## 🔥 Симуляция инцидентов

### Включить режим ошибок (~50% запросов с ошибкой 500):

```bash
curl -X POST http://localhost:8080/enable-errors
```

### Включить режим замедления (все запросы медленные):

```bash
curl -X POST http://localhost:8080/enable-slow
```

### Отключить все режимы:

```bash
curl -X POST http://localhost:8080/disable-modes
```

### Остановить генератор нагрузки:

```bash
docker stop k6
```

### Запустить генератор нагрузки:

```bash
docker start k6
```

---

## 📁 Структура проекта

```
metrics-workshop/
├── app/                    # Spring Boot приложение
├── prometheus/             # Конфигурация Prometheus и алерты
├── grafana/                # Дашборды и datasources
├── alertmanager/           # Конфигурация Alertmanager
├── k6/                     # Скрипт нагрузочного тестирования
├── filebeat/               # Сбор логов
└── docker-compose.yml      # Конфигурация стека
```

---

## 📌 Полезные эндпоинты приложения

| Эндпоинт | Метод | Описание |
|----------|-------|----------|
| `/fast` | GET | Быстрый ответ |
| `/slow` | GET | Ответ с задержкой 300–500ms |
| `/error` | GET | ~30% ошибок (500) |
| `/actuator/prometheus` | GET | Метрики Prometheus |
| `/enable-errors` | POST | Включить режим ошибок |
| `/enable-slow` | POST | Включить режим замедления |
| `/disable-modes` | POST | Отключить все режимы |

