CREATE KEYSPACE IF NOT EXISTS local
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

CREATE TABLE IF NOT EXISTS local.cpu_metrics_by_device (
    deviceId TEXT,
    windowStart TIMESTAMP,
    windowEnd TIMESTAMP,
    percentile95 DOUBLE,
    lastUpdated TIMESTAMP,
    PRIMARY KEY ((deviceId), windowEnd)
) WITH CLUSTERING ORDER BY (windowEnd DESC);

CREATE TABLE IF NOT EXISTS local.cpu_metrics_by_window_end (
    deviceId TEXT,
    windowStart TIMESTAMP,
    windowEnd TIMESTAMP,
    percentile95 DOUBLE,
    lastUpdated TIMESTAMP,
    PRIMARY KEY ((windowEnd), deviceId)
) WITH CLUSTERING ORDER BY (deviceId DESC);
