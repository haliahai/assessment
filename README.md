# Warehouse Monitoring

Two reactive services:
- **warehouse-service** receives sensor readings over UDP and publishes them to Kafka.
- **central-monitoring-service** consumes them and logs an `ALARM` when a value is above its threshold.

```
sensors --UDP 3344/3355--> warehouse-service --Kafka (warehouse.measurements)--> central-monitoring-service --> ALARM log
```

| Sensor      | UDP port | Payload                  | Threshold |
|-------------|----------|--------------------------|-----------|
| Temperature | 3344     | `sensor_id=t1; value=30` | 35 °C     |
| Humidity    | 3355     | `sensor_id=h1; value=40` | 50 %      |

Stack: Java 21, Spring Boot 3.5, Project Reactor, Reactor Netty (UDP), reactor-kafka, Kafka (KRaft), Testcontainers.

## Run

Needs Java 21, Maven and Docker.

```bash
mvn clean package -DskipTests
docker compose up -d --build      # Kafka, warehouse-1 (3344/3355), warehouse-2 (4344/4355), central
docker compose logs -f central-monitoring-service
```

### Send readings

```bash
echo "sensor_id=t1; value=30"   | nc -u -w1 localhost 3344   # ok
echo "sensor_id=t1; value=35"   | nc -u -w1 localhost 3344   # ok, equal is not an alarm
echo "sensor_id=t1; value=36"   | nc -u -w1 localhost 3344   # ALARM
echo "sensor_id=h1; value=50.5" | nc -u -w1 localhost 3355   # ALARM
echo "sensor_id=t1; value=41"   | nc -u -w1 localhost 4344   # ALARM from warehouse-2
echo "garbage"                  | nc -u -w1 localhost 3355   # WARN in warehouse log, dropped
```

### Tests

```bash
mvn clean verify
```

## Configuration

`application.yml`. Every setting can be overridden with an env var or `--key=value`.

| Property | Env var | Default |
|---|---|---|
| `warehouse.id` | `WAREHOUSE_ID` | `warehouse-1` |
| `warehouse.ports.temperature` / `.humidity` | `TEMPERATURE_PORT` / `HUMIDITY_PORT` | 3344 / 3355 |
| `warehouse.buffer-size` / `max-in-flight` | | 1024 / 256 |
| `monitoring.thresholds.temperature` / `.humidity` | `TEMPERATURE_THRESHOLD` / `HUMIDITY_THRESHOLD` | 35 / 50 |
| `monitoring.kafka.group-id` / `session-timeout` | | `central-monitoring` / 10s |
| `*.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `*.kafka.topic` | `MEASUREMENTS_TOPIC` | `warehouse.measurements` |
