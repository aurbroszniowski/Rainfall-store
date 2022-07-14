/*
 * Copyright (c) 2014-2019 Aurélien Broszniowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rainfall.store.client;

import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.MetricsLog;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.MetricsRec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.rainfall.store.data.CompressionFormat.RAW;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class DefaultStoreClientServiceTest extends AbstractStoreClientServiceTest {

  private MockStore store;

  @Override
  StoreClientService createService() {
    store = new MockStore();
    return new DefaultStoreClientService(store, compressionService(RAW));
  }

  @Override
  void addTestCase(String caseName) {
    store.addTestCase(caseName, mock(TestCase.class));
  }

  void checkRuns(String caseName, TestRun expectedRun) {
    assertThat(store.runs.values(), contains(expectedRun));
  }

  @Override
  void checkClientJobs(ClientJob expectedClientJob) {
    assertThat(store.clientJobs, contains(expectedClientJob));
  }

  @Override
  void checkOutputs(List<OperationOutput> expectedOutputs) {
    matchOutputs(expectedOutputs, store.outputs);
  }

  @Override
  void setBaseline(long baselineId) {
    //noop
  }

  @Override
  void checkChangeReport(ChangeReport changeReport, double threshold) {
    assertThat(changeReport, is(new ChangeReport(threshold)));
  }

  @Override
  void checkLogs(StatsLog expectedLog) throws IOException {
    matchOutputs(expectedLog, store.logs.get(0));
  }

  private static class MockStore implements StoreClient {

    private final Map<String, TestCase> testCases = new HashMap<>();
    private final Map<Long, TestRun> runs = new HashMap<>();
    private final List<ClientJob> clientJobs = new ArrayList<>();
    private final List<OperationOutput> outputs = new ArrayList<>();
    private final List<StatsLog> logs = new ArrayList<>();

    @Override
    public void addTestCase(String uniqueName, TestCase testCase) {
      testCases.put(uniqueName, testCase);
    }

    @Override
    public long addRun(String caseName, TestRun run) {
      Objects.requireNonNull(testCases.get(caseName), "Test not found: " + caseName);
      long ID = runs.size() + 1;
      runs.put(ID, run);
      return ID;
    }

    @Override
    public long addClientJob(long runId, ClientJob job) {
      clientJobs.add(job);
      return clientJobs.size();
    }

    @Override
    public long addOutput(long jobId, OperationOutput output) {
      outputs.add(output);
      return outputs.size();
    }

    @Override
    public long addStatsLog(long runId, StatsLog log) {
      logs.add(log);
      return logs.size();
    }

    @Override
    public long addMetricsLog(MetricsLog metricsLog) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean setStatus(long runId, TestRun.Status status) {
      TestRun run = runs.get(runId);
      if (run == null) {
        return false;
      } else {
        TestRun updated = updateStatus(run, status);
        runs.put(runId, updated);
        return true;
      }
    }

    @Override
    public List<MetricsRec> listMetricsRec() {
      throw new UnsupportedOperationException();
    }

    private TestRun updateStatus(TestRun run, TestRun.Status status) {
      return TestRun.builder()
          .className(run.getClassName())
          .version(run.getVersion())
          .checksum(run.getChecksum())
          .status(status)
          .build();
    }

    @Override
    public ChangeReport checkRegression(long runId, double threshold) {
      return new ChangeReport(threshold);
    }
  }
}
