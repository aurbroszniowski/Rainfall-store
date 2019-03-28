package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.ClientJob;
import com.terracotta.qa.perf.core.OperationOutput;
import com.terracotta.qa.perf.core.StatsLog;
import com.terracotta.qa.perf.core.TestCase;
import com.terracotta.qa.perf.core.TestRun;

public interface PerfStoreWriter {

  void addTestCase(String uniqueName, TestCase testCase);

  long addRun(String caseName, TestRun run);

  long addClientJob(long runId, ClientJob job);

  long addOutput(long jobId, OperationOutput output);

  long addStatsLog(long runId, StatsLog log);

  boolean setStatus(long runId, TestRun.Status status);
}
