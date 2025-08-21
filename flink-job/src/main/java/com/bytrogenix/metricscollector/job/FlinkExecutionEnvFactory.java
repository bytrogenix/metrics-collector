package com.bytrogenix.metricscollector.job;

import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.core.execution.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import static org.apache.flink.configuration.ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION;

public class FlinkExecutionEnvFactory {

    public StreamExecutionEnvironment get() {
        var config = new Configuration();
        config.set(StateBackendOptions.STATE_BACKEND, "rocksdb");
        config.setString("state.backend.rocksdb.memory.managed", "true");
        config.set(CheckpointingOptions.INCREMENTAL_CHECKPOINTS, true);
        config.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "file:///opt/flink/checkpoints");

        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(60_000, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
        env.getCheckpointConfig().setExternalizedCheckpointRetention(RETAIN_ON_CANCELLATION);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.configure(config);

        return env;
    }
}
