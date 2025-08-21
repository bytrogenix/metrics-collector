# metrics-collector

Design and implement the core processing logic for a high-throughput, low-latency real-time analytics pipeline,
that computes CPU usage per IOT device.

Input is continuous stream of events:
```json
{
    "deviceId": "device-11",
    "timestamp": 12385439653,
    "cpuUsage": 78.986
}
```


## Run

1. Build apps: `./gradlew :web-app:bootJar :flink-job:shadowJar`
2. Start docker containers: `docker-compose up --build --detach`

Watch Kafka topic messages via control center: http://localhost:9021
Watch Flink job via Dashboard: http://localhost:8081

Rest API:
- http://localhost:8080/metrics/top/10
- http://localhost:8080/metrics/devices/device-20



## Overview

Technology selection:
- used Flink instead of Spark for real-time processing
- used Cassandra as storage, as we have infinitely growing data with write-heavy load
- used Spring Boot as rest api layer
- structured project as multi-module Gradle project with two (Flink, Spring) apps, and one commons library
- all apps and their deps are running inside docker containers

Data Model:
 - `cpu_metrics_by_device` table: partitioned by `deviceId` with `windowEnd` clustering index, so that `where deviceId=X order by windowEnd` query is efficient
 - `cpu_metrics_by_window_end` table: same table but partitioned by `windowEnd` to be able to filter by latest timestamp
   - this is not correct and won't allow to efficiently do topN queries
   - ideally we want to have an extra CDC pipeline to stream results into more appropriate storage for such queries (ex. Elasticsearch)

General architecture:
 - we generate fake events into `iot.device.events.dev` Kafka topic in JSON format, partitioned by `deviceId` to avoid Flink shuffling
 - our Flink job consumes the events, keys them by `deviceId` and aggregates using TDigest approximation
   - since amount of input data can be huge, we might end up with millions of events inside our sliding window, and so we will run out of memory
   - with TDigest, we can use incremental aggregation by storing only a compressed TDigest object
   - then calculate required percentile once window end event happens
 - as timestamp, we use input event timestamp
 - all the events, that arrived up to 5m late, will be included in window percentile calculation
 - events, that are super later, are pushed into `iot.cpu.metrics.tooLate` Kafka topic
   - if needed, we can have an external reconciliation batch job to adjust existing results
 - since we have 2 cassandra tables, we sink result into both of them


### Exactly-once

Flink checkpointing ensures exactly once semantics and Cassandra inserts are idempotent, so duplicates won't happen on retry.

### Late/out-of-order events

Allowed lateness: 5 minutes. Late events revise the same window row.
Too-late events are send to side output (Kafka topic), we can run batch correction job to re-aggregate.



## Scalability

We can scale all the selected components horizontally:
 - Kafka: throughput is decided by number of partitions, so we should have estimated it initially with future growth. As a last resort we can re-partition topic to increase partition count
 - Cassandra: we can increase number of nodes/virtual nodes
 - Flink: we can increase number of task nodes and parallelism
 - Spring app: we can increase number of instances or pods


## Fault tolerance

All the infra components have availability > consistency with data replication.

For Flink specifically:
 - in case of job failure, it will be retried from the checkpoint
 - if node fails: any other node will take it's part, as Kafka offsets won't be commited and processing will be retried


## Performance tuning

For Flink specifically:
- set correct Parallelism: ~ to number of Kafka partitions, limiting by available slots
- fine tune buffer timeout: more buffer -> more throughput, higher latency
- set correct Checkpoint interval, depending on backpressure
- `allowed lateness` determines how long to keep windows open for late events: bigger number -> more latency
- in prod, we by default will have persistent storage for checkpoints: in-memory is faster, but state might be lost
- restart strategy must be at least fixedDelay to reduce latency
- have async i/o for Cassandra not to block operator


## Monitoring and Testing

Testing
 - Flink job: have local integration test with docker to cover job end-to-end: compare expected outputs to generated in Cassandra
 - Rest api: standard functional tests for controller routes with docker Cassandra
 - business logic java classes: unit tests
 - load testing: end-to-end Kafka producer simulation on real testing environment


Monitoring
 - Metrics: Prometheus, Graphite -> Grafana
 - Dashboard for Flink: backpressure, checkpoint duration, watermark lag, task busy time


Alerting
 - Flink checkpoint, job failures
 - Kafka consumer lag > threshold
 - API error rate
 - Cassandra pending compactions



## Out-of-scope

I've run into some issues with Flink versions and libs support, which took more time than expected,
so wasn't able to write tests and skipped quite a few features due to time constraints.

There's much more that can be done:
 - kafka
   - use schema registry
   - use protobuf instead of json
 - flink
   - have proper `flink-conf.yaml` config file
   - have proper config values injection instead of hardcoded strings
   - async batch insertion into cassandra instead of `CassandraSink`
   - since it's an assignment, i didn't properly split the code into reusable components
 - api
   - have reactive rest api processing for increased throughput
   - cassandra shouldn't be used for top N query, but an appropriate storage, like Elasticsearch
