package com.terracotta.qa.perf.store;

import java.util.List;
import java.util.Optional;

public interface PerfStoreReader {

  Optional<TestCaseRec> getTestCase(String uniqueName);

  List<TestCaseRec> getTestCases();


  Optional<RunRec> getRun(long id);

  List<RunRec> getRuns(String caseName);


  Optional<ClientJobRec> getClientJob(long id);

  List<ClientJobRec> getClientJobs(long runId);


  Optional<OutputRec> getOutput(long id);

  List<OutputRec> getOutputs(long jobId);

  List<OutputRec> getOutputsForOperation(long runId, String operation);


  Optional<StatsRec> getStatsLog(long id);

  List<StatsRec> getStats(long runId);

  List<StatsRec> getStats(long runId, String localhost);

  Optional<Long> getLastBaselineID();
}
