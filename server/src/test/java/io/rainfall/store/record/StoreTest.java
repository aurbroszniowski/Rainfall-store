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

package io.rainfall.store.record;

import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import org.junit.Test;

import com.google.gson.Gson;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.rainfall.store.core.TestRun.Status.COMPLETE;
import static io.rainfall.store.core.TestRun.Status.INCOMPLETE;
import static io.rainfall.store.data.Payload.raw;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class StoreTest {

  private final TestCase testCase = TestCase.builder()
      .description("description")
      .build();

  private final TestRun run = TestRun.builder()
      .version("v1")
      .className("TestClass")
      .checksum("00000")
      .build();

  private final ClientJob job = ClientJob.builder()
      .clientNumber(1)
      .host("localhost")
      .symbolicName("localhost-1")
      .details("details")
      .build();

  private final OperationOutput output1 = OperationOutput.builder()
      .operation("GET")
      .data("DATA")
      .build();

  private final OperationOutput output2 = OperationOutput.builder()
      .operation("MISS")
      .format("hlog")
      .data("LOG")
      .build();

  private final StatsLog log = StatsLog.builder()
      .host("localhost")
      .data("1111")
      .build();

  protected abstract Store createStore() throws Exception;

  @Test
  public void testGetNonExistentTestCase() throws Exception {
    try (Store store = createStore()) {
      Optional<TestCaseRec> record = store.getTestCase("NoSuchTest");
      assertFalse(record.isPresent());
    }
  }

  @Test
  public void testAddTestCase() throws Exception {
    try (Store store = createStore()) {
      Instant before = now();
      store.addTestCase("MyTest", testCase);
      Optional<TestCaseRec> result = store.getTestCase("MyTest");
      assertTrue(result.isPresent());

      TestCaseRec rec = result.get();
      assertThat("MyTest", is(rec.getID()));
      assertTimeStamp(rec, before);

      TestCase actual = rec.getValue();
      assertNotNull(actual);
      assertEquals("description", actual.getDescription());
      assertEquals(testCase, actual);
    }
  }

  @Test(expected = DuplicateNameException.class)
  public void testAddTestCaseWithDuplicateUniqueName() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      TestCase sameName = TestCase.builder()
          .description("otherDescription")
          .build();
      store.addTestCase("MyTest", sameName);
      fail();
    }
  }

  private void assertTimeStamp(Rec<?, ?> rec, Instant before) {
    assertThat(rec.getTimeStamp(), greaterThanOrEqualTo(before.getEpochSecond()));
    assertThat(rec.getTimeStamp(), lessThanOrEqualTo(now().getEpochSecond()));
  }

  @Test
  public void testGetTestCases() throws Exception {
    try (Store store = createStore()) {
      assertThat(store.getTestCases(), empty());

      TestCase case1 = TestCase.builder()
          .description("description1")
          .build();
      store.addTestCase("Test1", case1);

      TestCase case2 = TestCase.builder()
          .description("description2")
          .build();
      store.addTestCase("Test2", case2);

      List<TestCaseRec> recs = store.getTestCases();

      List<String> ids = recs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(ids,
          containsInAnyOrder("Test1", "Test2"));

      List<TestCase> cases = recs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(cases,
          containsInAnyOrder(case1, case2));
    }
  }

  @Test
  public void testGetNonExistentRun() throws Exception {
    try (Store store = createStore()) {
      Optional<RunRec> record = store.getRun(0L);
      assertFalse(record.isPresent());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testAddRunToNonExistentTestCase() throws Exception {
    try (Store store = createStore()) {
      TestRun run = TestRun.builder()
          .build();
      store.addRun("NoSuchTest", run);
    }
  }

  @Test
  public void testAddRunToTestCase() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      Instant before = now();
      long runId = store.addRun("MyTest", run);

      Optional<RunRec> result = store.getRun(runId);
      assertTrue(result.isPresent());
      RunRec rec = result.get();
      assertThat(rec.getID(), is(runId));
      assertTimeStamp(rec, before);
      assertThat(rec.getParentID(), is("MyTest"));

      TestRun actual = rec.getValue();
      assertEquals("v1", actual.getVersion());
      assertEquals("TestClass", actual.getClassName());
      assertEquals("00000", actual.getChecksum());
      assertEquals(INCOMPLETE, actual.getStatus());
      assertFalse(actual.isBaseline());
      assertEquals(run, actual);
    }
  }

  /**
   * Backward compatibility test.
   */
  @Test
  public void testAddRunWithUndefinedStatus() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);

      Gson gson = new Gson();
      String json = "{'version':'v1','className':'TestClass','checksum':'00000'}";
      TestRun runWithoutStatus = gson.fromJson(json, TestRun.class);

      long runId = store.addRun("MyTest", runWithoutStatus);

      Optional<RunRec> result = store.getRun(runId);
      assertTrue(result.isPresent());

      RunRec rec = result.get();
      assertThat(rec.getID(), is(runId));
      assertThat(rec.getParentID(), is("MyTest"));

      TestRun actual = rec.getValue();
      assertEquals(this.run, actual);
    }
  }

  @Test
  public void testSetStatusOfNonExistentRun() throws Exception {
    try (Store store = createStore()) {
      boolean success = store.setStatus(1L, COMPLETE);
      assertFalse(success);
    }
  }

  @Test
  public void testSetStatus() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);

      boolean success = store.setStatus(runId, COMPLETE);
      assertTrue(success);

      Optional<RunRec> result = store.getRun(runId);
      assertTrue(result.isPresent());
      TestRun actual = result.get().getValue();
      assertEquals(COMPLETE, actual.getStatus());
    }
  }

  @Test
  public void testSetNonExistentRunAsBaseline() throws Exception {
    try (Store store = createStore()) {
      boolean success = store.setBaseline(1L, true);
      assertFalse(success);
    }
  }

  @Test
  public void testSetBaseline() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);

      boolean success = store.setBaseline(runId, true);
      assertTrue(success);

      Optional<RunRec> result = store.getRun(runId);
      assertTrue(result.isPresent());
      TestRun run = result.get().getValue();
      assertTrue(run.isBaseline());
    }
  }

  @Test
  public void testGetLastBaselineID() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId1 = store.addRun("MyTest", run);

      Optional<Long> notFound = store.getLastBaselineID("MyTest");
      assertFalse(notFound.isPresent());

      store.setBaseline(runId1, true);

      Optional<Long> first = store.getLastBaselineID("MyTest");
      assertTrue(first.isPresent());
      assertThat(first.get(), is(runId1));

      long runId2 = store.addRun("MyTest", run);

      assertEquals(store.getLastBaselineID("MyTest"), first);
      store.setBaseline(runId2, true);

      Optional<Long> second = store.getLastBaselineID("MyTest");
      assertTrue(second.isPresent());
      assertThat(second.get(), is(runId2));

      store.setBaseline(runId2, false);
      assertEquals(store.getLastBaselineID("MyTest"), first);

      store.setBaseline(runId1, false);
      assertEquals(store.getLastBaselineID("MyTest"), notFound);
    }
  }

  @Test
  public void testGetRunsForNonExistentTestCase() throws Exception {
    try (Store store = createStore()) {
      List<RunRec> runs = store.getRuns("NoSuchTest");
      assertThat(runs, empty());
    }
  }

  @Test
  public void testGetRunsForTestCase() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);

      assertThat(store.getRuns("MyTest"), empty());

      TestRun run1 = TestRun.builder()
          .version("v1")
          .className("TestClass")
          .checksum("00000")
          .build();
      long id1 = store.addRun("MyTest", run1);

      TestRun run2 = TestRun.builder()
          .version("v2")
          .className("OtherTestClass")
          .checksum("11111")
          .build();
      long id2 = store.addRun("MyTest", run2);

      List<RunRec> recs = store.getRuns("MyTest");

      List<Long> ids = recs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(ids, containsInAnyOrder(id1, id2));

      List<TestRun> runs = recs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(runs, containsInAnyOrder(run1, run2));
    }
  }

  @Test
  public void testGetNonExistentClientJob() throws Exception {
    try (Store store = createStore()) {
      Optional<ClientJobRec> record = store.getClientJob(-1L);
      assertFalse(record.isPresent());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testAddClientJobToNonExistentRun() throws Exception {
    try (Store store = createStore()) {
      store.addClientJob(-1L, job);
    }
  }

  @Test
  public void testAddClientJobToRun() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);

      Instant before = now();
      long jobId = store.addClientJob(runId, job);

      Optional<ClientJobRec> result = store.getClientJob(jobId);
      assertTrue(result.isPresent());

      ClientJobRec rec = result.get();
      assertThat(rec.getID(), is(jobId));
      assertTimeStamp(rec, before);
      assertThat(rec.getParentID(), is(runId));

      ClientJob actual = rec.getValue();
      assertEquals(1, actual.getClientNumber());
      assertEquals("localhost", actual.getHost());
      assertEquals("localhost-1", actual.getSymbolicName());
      assertEquals("details", actual.getDetails());
      assertEquals(job, actual);
    }
  }

  @Test
  public void testGetClientJobsForNonExistentRun() throws Exception {
    try (Store store = createStore()) {
      List<ClientJobRec> jobs = store.getClientJobs(-1L);
      assertThat(jobs, empty());
    }
  }

  @Test
  public void testGetClientJobsForRun() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      assertThat(store.getOutputs(runId), empty());

      ClientJob job1 = ClientJob.builder()
          .clientNumber(1)
          .host("localhost")
          .symbolicName("localhost")
          .details("getting")
          .build();
      long id1 = store.addClientJob(runId, job1);

      ClientJob job2 = ClientJob.builder()
          .clientNumber(2)
          .host("127.0.0.1")
          .symbolicName("127.0.0.1")
          .details("adding")
          .build();
      long id2 = store.addClientJob(runId, job2);

      List<ClientJobRec> recs = store.getClientJobs(runId);

      List<Long> ids = recs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(ids, containsInAnyOrder(id1, id2));

      List<ClientJob> jobs = recs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(jobs, containsInAnyOrder(job1, job2));
    }
  }

  @Test
  public void testGetNonExistentOutput() throws Exception {
    try (Store store = createStore()) {
      Optional<OutputRec> record = store.getOutput(-1L);
      assertFalse(record.isPresent());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testAddOutputToNonExistentJob() throws Exception {
    try (Store store = createStore()) {
      store.addOutput(-1L, output1);
    }
  }

  @Test
  public void testAddOutputToJob() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      long jobId = store.addClientJob(runId, job);

      Instant before = now();
      long outputId = store.addOutput(jobId, output1);

      Optional<OutputRec> result = store.getOutput(jobId);
      assertTrue(result.isPresent());

      OutputRec rec = result.get();
      assertThat(rec.getID(), is(outputId));
      assertTimeStamp(rec, before);
      assertThat(rec.getParentID(), is(jobId));

      OperationOutput actual = rec.getValue();
      assertEquals("GET", actual.getOperation());
      assertEquals("hlog", actual.getFormat());
      assertEquals(raw("DATA"), actual.getPayload());
      assertEquals(output1, actual);
    }
  }

  @Test
  public void testGetOutputsForNonExistentJob() throws Exception {
    try (Store store = createStore()) {
      List<OutputRec> outputs = store.getOutputs(-1L);
      assertThat(outputs, empty());
    }
  }

  @Test
  public void testGetOutputsForJob() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      long jobId = store.addClientJob(runId, job);
      assertThat(store.getOutputs(jobId), empty());

      long id1 = store.addOutput(jobId, output1);
      long id2 = store.addOutput(jobId, output2);
      List<OutputRec> recs = store.getOutputs(jobId);

      List<Long> ids = recs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(ids, containsInAnyOrder(id1, id2));

      List<OperationOutput> outputs = recs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(outputs, containsInAnyOrder(output1.unloaded(), output2.unloaded()));
    }
  }

  @Test
  public void testGetNonExistentStatsLog() throws Exception {
    try (Store store = createStore()) {
      Optional<StatsRec> record = store.getStatsLog(-1L);
      assertFalse(record.isPresent());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testAddStatsToNonExistentRun() throws Exception {
    try (Store store = createStore()) {
      store.addStatsLog(-1L, log);
    }
  }

  @Test
  public void testAddStatsToRun() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      Long runId = store.addRun("MyTest", run);

      Instant before = now();
      long logId = store.addStatsLog(runId, log);

      Optional<StatsRec> result = store.getStatsLog(logId);
      assertTrue(result.isPresent());

      StatsRec rec = result.get();
      assertThat(rec.getID(), is(logId));
      assertTimeStamp(rec, before);
      assertThat(rec.getParentID(), is(runId));

      StatsLog actual = rec.getValue();
      assertEquals(raw("1111"), actual.getPayload());
      assertEquals(log, actual);
    }
  }

  @Test
  public void testGetStatsForRun() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      assertThat(store.getStats(runId), empty());

      StatsLog log1 = StatsLog.builder()
          .host("localhost")
          .data("1111")
          .build();
      long id1 = store.addStatsLog(runId, log1);

      StatsLog log2 = StatsLog.builder()
          .host("127.0.0.1")
          .data("2222")
          .build();
      long id2 = store.addStatsLog(runId, log2);

      List<StatsRec> recs = store.getStats(runId);

      List<Long> ids = recs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(ids, containsInAnyOrder(id1, id2));

      List<StatsLog> logs = recs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(logs, containsInAnyOrder(log1, log2));
    }
  }

  @Test
  public void testGetStatsForRunAndHost() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      assertThat(store.getStats(runId), empty());

      StatsLog log1 = StatsLog.builder()
          .host("localhost")
          .data("1111")
          .build();
      long id1 = store.addStatsLog(runId, log1);

      StatsLog log2 = StatsLog.builder()
          .host("remote.org")
          .data("2222")
          .build();
      long id2 = store.addStatsLog(runId, log2);

      StatsLog log3 = StatsLog.builder()
          .host("localhost")
          .data("3333")
          .build();
      long id3 = store.addStatsLog(runId, log3);

      List<StatsRec> localhostRecs = store.getStats(runId, "localhost");

      List<Long> localhostIds = localhostRecs.stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(localhostIds, containsInAnyOrder(id1, id3));

      List<StatsLog> localhostLogs = localhostRecs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(localhostLogs, containsInAnyOrder(log1, log3));

      List<StatsRec> remoteRecs = store.getStats(runId, "remote.org");
      assertThat(remoteRecs.size(), is(1));
      StatsRec remoteRec = remoteRecs.get(0);
      assertThat(remoteRec.getID(), is(id2));
      assertThat(remoteRec.getValue(), is(log2));
    }
  }

  @Test
  public void testGetOperationsForRunEmpty() throws Exception {
    try (Store store = createStore()) {
      assertThat(store.getOperationsForRun(1L), is(empty()));
    }
  }

  @Test
  public void testGetOperationsForRun() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      long jobId = store.addClientJob(runId, job);
      store.addOutput(jobId, output1);
      store.addOutput(jobId, output2);
      assertThat(
          store.getOperationsForRun(runId),
          containsInAnyOrder("GET", "MISS")
      );
    }
  }

  @Test
  public void testGetOutputsForOperation() throws Exception {
    try (Store store = createStore()) {
      store.addTestCase("MyTest", testCase);
      long runId = store.addRun("MyTest", run);
      long jobId = store.addClientJob(runId, job);

      long id1 = store.addOutput(jobId, output1);
      long id2 = store.addOutput(jobId, output2);

      List<OutputRec> outputsForGet = store.getOutputsForOperation(runId, "GET");
      assertThat(outputsForGet.stream().map(Rec::getID).collect(toList()), containsInAnyOrder(id1));
      assertThat(outputsForGet.stream().map(Rec::getValue).collect(toList()), containsInAnyOrder(output1));

      List<OutputRec> outputsForMiss = store.getOutputsForOperation(runId, "MISS");
      assertThat(outputsForMiss.stream().map(Rec::getID).collect(toList()), containsInAnyOrder(id2));
      assertThat(outputsForMiss.stream().map(Rec::getValue).collect(toList()), containsInAnyOrder(output2));
    }
  }
}
