package io.rainfall.store.record;


import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;

public interface StoreWriter {

  void addTestCase(String uniqueName, TestCase testCase);

  long addRun(String caseName, TestRun run);

  long addClientJob(long runId, ClientJob job);

  long addOutput(long jobId, OperationOutput output);

  long addStatsLog(long runId, StatsLog log);

  boolean setStatus(long runId, TestRun.Status status);
}
