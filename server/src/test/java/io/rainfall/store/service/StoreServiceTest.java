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

package io.rainfall.store.service;

import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.data.CompressionService;
import io.rainfall.store.data.Payload;
import io.rainfall.store.record.ClientJobRec;
import io.rainfall.store.record.DuplicateNameException;
import io.rainfall.store.record.OutputRec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.StatsRec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.TestCaseRec;
import io.rainfall.store.service.hdr.HdrData;
import io.rainfall.store.service.hdr.HistogramService;
import org.junit.Test;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.rainfall.store.core.TestRun.Status.FAILED;
import static io.rainfall.store.data.CompressionFormat.LZ4;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static io.rainfall.store.data.Payload.toUtfString;
import static io.rainfall.store.service.hdr.Percentile.MEDIAN;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StoreServiceTest {

  private static final TestCase testCase = TestCase.builder()
      .description("Case 1")
      .build();

  private static final TestRun run = TestRun.builder()
      .version("v1")
      .build();

  private static final ClientJob job = ClientJob.builder()
      .clientNumber(1)
      .host("localhost")
      .details("details")
      .build();

  private static final OperationOutput output = OperationOutput.builder()
      .operation("GET")
      .data("DATA")
      .build();

  private static final StatsLog log = StatsLog.builder()
      .host("localhost")
      .data("1111")
      .build();

  private static final HdrData HDR_LOG = HdrData.builder()
      .addStartTime(0L)
      .addTps(1L)
      .addMean(0.0)
      .addError(1.0)
      .roundedPercentiles(singletonMap(MEDIAN, 100L))
      .addPercentile(0.5, 100)
      .fixedPercentileValues(nCopies(10, 0L))
      .addTimedPercentile(MEDIAN, 1.0)
      .build();

  private static final Gson gson = new Gson();

  private static StoreService service() {
    Store store = mock(Store.class);

    when(store.getTestCase(anyString()))
        .thenReturn(empty());
    when(store.getTestCase("Test1"))
        .thenReturn(of(new TestCaseRec("Test1", testCase, 0L)));
    doThrow(new DuplicateNameException("Test1"))
        .when(store)
        .addTestCase(anyString(), argThat(not(equalTo(testCase))));
    when(store.getTestCases())
        .thenReturn(asList(
            new TestCaseRec("Test2", testCase, 1),
            new TestCaseRec("Test1", testCase, 0)
        ));

    when(store.getRuns("Test1"))
        .thenReturn(asList(
            new RunRec("Test2", 2L, run, 1L),
            new RunRec("Test1", 1L, run, 0L)
        ));
    when(store.getRun(anyLong()))
        .thenReturn(empty());
    when(store.getRun(1L))
        .thenReturn(of(new RunRec("Test1", 1L, run, 0L)));
    when(store.getRun(2L))
        .thenReturn(of(new RunRec("Test1", 2L, run, 0L)));
    when(store.getRun(3L))
        .thenReturn(of(new RunRec("Test2", 3L, run, 1L)));
    when(store.addRun(anyString(), any()))
        .thenAnswer(invocation -> {
          Object testName = invocation.getArguments()[0];
          if ("Test1".equals(testName)) {
            return 2L;
          } else {
            throw new IllegalArgumentException("TestCase does not exists: " + testName);
          }
        });
    when(store.getLastBaselineID(anyString()))
        .thenReturn(Optional.empty());

    when(store.getClientJobs(1L))
        .thenReturn(singletonList(new ClientJobRec(1L, 1L, job, 0L)));
    when(store.getClientJob(anyLong()))
        .thenReturn(empty());
    when(store.getClientJob(1L))
        .thenReturn(of(new ClientJobRec(1L, 1L, job, 0L)));
    when(store.addClientJob(anyLong(), any()))
        .thenReturn(2L);

    when(store.getOutputs(1L))
        .thenReturn(singletonList(
            new OutputRec(1L, 1L, output.unloaded(), 0L)));
    when(store.getOutput(anyLong()))
        .thenReturn(empty());
    when(store.getOutput(1L))
        .thenReturn(of(new OutputRec(1L, 1L, compressed(output), 0L)));
    when(store.getOutput(2L))
        .thenReturn(of(new OutputRec(1L, 1L, compressed(output), 0L)));
    when(store.addOutput(anyLong(), any()))
        .thenReturn(2L);

    when(store.getStats(1L))
        .thenReturn(singletonList(new StatsRec(1L, 1L, log, 0L)));
    when(store.getStats(anyLong(), anyString()))
        .thenReturn(singletonList(new StatsRec(1L, 1L, log, 0L)));
    when(store.getStatsLog(anyLong()))
        .thenReturn(empty());
    when(store.getStatsLog(1L))
        .thenReturn(of(new StatsRec(1L, 1L, log, 0L)));
    when(store.addStatsLog(anyLong(), any()))
        .thenReturn(2L);

    when(store.getOperationsForRun(1L))
        .thenReturn(singleton("GET"));
    when(store.getOperationsForRun(2L))
        .thenReturn(Stream.of("GET", "MISS").collect(toSet()));
    when(store.getOperationsForRun(3L))
        .thenReturn(singleton("MISS"));

    HistogramService histogramService = mock(HistogramService.class);
    when(histogramService.readHdrData(any()))
        .thenReturn(HDR_LOG);
    when(histogramService.aggregateHdrData(any()))
        .thenReturn(HDR_LOG);
    when(histogramService.comparePercentiles(any(), any()))
        .thenReturn(0.0);

    return new StoreService(store, histogramService);
  }

  @SuppressWarnings("SameParameterValue")
  private static OperationOutput compressed(OperationOutput output) {
    try {
      Payload payload = output.getPayload();
      CompressionService compressionService = compressionService(LZ4);
      Payload compressed = compressionService.compress(payload.getData());
      return output.withPayload(compressed);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void testGetNonExistentTestCase() {
    assertNotFound(service().getTestCase("NoSuchName"),
        "Test ID not found: NoSuchName.");
  }

  private void assertNotFound(Result notFound, String msg) {
    assertThat(notFound, is(new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
        singletonMap("msg", msg))));
  }

  @Test
  public void testGetExistingTestCase() {
    Result found = service()
        .getTestCase("Test1");
    assertThat(found, is(new Result(HTTP_OK, APPLICATION_JSON,
        new TestCaseRec("Test1", testCase, 0))));
  }

  @Test
  public void testAddTestCase() {
    Result added = service()
        .addTestCase("Test1", "Case 1");
    assertThat(added, is(new Result(HTTP_CREATED, TEXT_HTML, "Test1")));
  }

  @Test
  public void testAddTestCaseWithInvalidName() {
    String illegalChars = ";/?:@=&\"<>#%{}|\\^~[]`";
    illegalChars.chars()
        .mapToObj(i -> (char)i)
        .map(String::valueOf)
        .forEach(this::testAddTestCaseWithInvalidName);
  }

  private void testAddTestCaseWithInvalidName(String uniqueName) {
    Result added = service()
        .addTestCase(uniqueName, "description");
    String msg = String.format("Invalid TestCase name: '%s'; must match '%s'.",
        uniqueName, StoreService.NAME_REGEX);
    assertThat(added, is(new Result(HTTP_BAD_REQUEST, TEXT_HTML, msg)));
  }

  @Test
  public void testAddTestCaseWithDuplicateName() {
    Result added = service()
        .addTestCase("Test1", "description");
    assertThat(added, is(new Result(HTTP_CONFLICT, TEXT_HTML,
        "Unique name already exists: Test1.")));
  }

  @Test
  public void testListTestCases() {
    List<TestCaseRec> infos = service()
        .listTestCases();
    assertThat(infos, contains(
        new TestCaseRec("Test1", testCase, 0),
        new TestCaseRec("Test2", testCase, 1)
    ));
  }

  @Test
  public void testGetRuns() {
    Result result = service().getRuns("Test1");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        asList(
            new RunRec("Test2", 2L, run, 1L),
            new RunRec("Test1", 1L, run, 0L)
        ))));
  }

  @Test
  public void testGetNonExistentRun() {
    assertNotFound(service().getRun("0"),
        "Run ID not found: 0.");
  }

  @Test
  public void testGetExistingRun() {
    Result found = service()
        .getRun("1");
    assertThat(found, is(new Result(HTTP_OK, APPLICATION_JSON,
        new RunRec("Test1", 1L, run, 0))));
  }

  @Test
  public void testCompareRuns() {
    Result result = service().compareRuns("1-2-3");
    assertThat(result, is(
        new Result(HTTP_OK, APPLICATION_JSON, asList(
            new RunRec("Test1", 1L, run, 0),
            new RunRec("Test1", 2L, run, 0),
            new RunRec("Test2", 3L, run, 1L)
        ))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddRunToNonExistentTestCase() {
    String json = gson.toJson(run, TestRun.class);
    service().addRun("NoSuchTest", json);
    fail();
  }

  @Test
  public void testAddRun() {
    String json = gson.toJson(run, TestRun.class);
    Result added = service()
        .addRun("Test1", json);
    assertThat(added,
        is(new Result(HTTP_CREATED, TEXT_HTML, 2L)));
  }

  @Test
  public void testSetStatusOfNonExistentRun() {
    StoreService service = service();
    Result result = service.setStatus("1", "FAILED");
    assertThat(result, is(
        new Result(HTTP_CONFLICT, TEXT_HTML, false)));
  }

  @Test
  public void testSetStatus() {
    TestRun.Builder builder = TestRun.builder();

    Store store = mock(Store.class);
    when(store.getRun(1L))
        .thenReturn(of(new RunRec("Test1", 1L, builder.build(), 0L)));
    doAnswer(answer -> {
      builder.status(FAILED);
      return true;
    }).when(store).setStatus(1L, FAILED);

    StoreService service = new StoreService(store, mock(HistogramService.class));
    Result result = service.setStatus("1", "FAILED");
    assertThat(result, is(
        new Result(HTTP_OK, TEXT_HTML, true)));
    assertThat(builder.build().getStatus(), is(FAILED));
  }

  @Test
  public void testSetNonExistentRunAsBaseline() {
    StoreService service = service();
    Result result = service.setBaseline("1", "true");
    assertThat(result, is(
        new Result(HTTP_CONFLICT, TEXT_HTML, false)));
  }

  @Test
  public void testSetBaseline() {
    TestRun.Builder builder = TestRun.builder();

    Store store = mock(Store.class);
    when(store.getRun(1L))
        .thenReturn(of(new RunRec("Test1", 1L, builder.build(), 0L)));
    doAnswer(answer -> {
      builder.baseline(true);
      return true;
    }).when(store).setBaseline(1, true);

    StoreService service = new StoreService(store, mock(HistogramService.class));
    Result result = service.setBaseline("1", "true");
    assertThat(result, is(
        new Result(HTTP_OK, TEXT_HTML, true)));
    assertThat(builder.build().isBaseline(), is(true));
  }

  @Test
  public void testCheckRegressionWithoutBaseline() {
    Result result = service().checkRegression("1", "0.0");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        new ChangeReport(0.0))));
  }

  @Test
  public void testCheckRegressionWithNonExistentRun() {
    Store store = mock(Store.class);
    when(store.getLastBaselineID("MyTest"))
        .thenReturn(Optional.of(1L));
    when(store.getRun(2L))
        .thenReturn(Optional.empty());
    StoreService perfService = new StoreService(store, mock(HistogramService.class));
    Result result = perfService.checkRegression("2", "0.0");
    assertThat(result, is(new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
        singletonMap("msg", "Run ID not found: 2"))));
  }

  @Test
  public void testCheckRegressionDifferentOperations() {
    Store store = mock(Store.class);
    RunRec runRec = mock(RunRec.class);
    when(runRec.getParentID())
        .thenReturn("Test1");
    when(store.getRun(2L))
        .thenReturn(Optional.of(runRec));
    when(store.getLastBaselineID("Test1"))
        .thenReturn(Optional.of(1L));
    when(store.getOperationsForRun(1L))
        .thenReturn(singleton("GET"));
    when(store.getOutputsForOperation(1L, "MISS"))
        .thenReturn(emptyList());
    when(store.getOperationsForRun(2L))
        .thenReturn(singleton("MISS"));

    HistogramService histogramService = mock(HistogramService.class);
    when(histogramService.comparePercentiles(any(), any()))
        .thenReturn(0.1);

    StoreService perfService = new StoreService(store, histogramService);
    Result result = perfService.checkRegression("2", "0.2");

    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        new ChangeReport(1L, 0.2, singletonMap("MISS", 0.1)))));
  }

  @Test
  public void testCheckRegressionChangeDetected() {
    Store store = mock(Store.class);
    RunRec runRec = mock(RunRec.class);
    when(runRec.getParentID())
        .thenReturn("Test1");
    when(store.getRun(2L))
        .thenReturn(Optional.of(runRec));
    when(store.getLastBaselineID("Test1"))
        .thenReturn(Optional.of(1L));
    when(store.getOperationsForRun(anyLong()))
        .thenReturn(singleton("GET"));

    HistogramService histogramService = mock(HistogramService.class);
    when(histogramService.comparePercentiles(any(), any()))
        .thenReturn(0.1);

    StoreService perfService = new StoreService(store, histogramService);
    Result result = perfService.checkRegression("2", "0.2");

    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        new ChangeReport(1L, 0.2, singletonMap("GET", 0.1)))));
  }

  @Test
  public void testGetClientJobs() {
    Result result = service().getClientJobs("1");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        singletonList(new ClientJobRec(1L, 1L, job, 0L)))));
  }

  @Test
  public void testGetStats() {
    Result result = service().getStats("1");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        singletonList(new StatsRec(1L, 1L, log, 0L)))));
  }

  @Test
  public void testGetStatsForRunAndHost() {
    Result result = service().getStats("1", "localhost");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        singletonList(new StatsRec(1L, 1L, log, 0L)))));
  }

  @Test
  public void testGetNonExistentJob() {
    assertNotFound(service().getClientJob("0"),
        "Client job ID not found: 0.");
  }

  @Test
  public void testGetExistingJob() {
    Result found = service()
        .getClientJob("1");
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON,
            new ClientJobRec(1L, 1L, job, 0L))));
  }

  @Test
  public void testAddClientJob() {
    String json = gson.toJson(job, ClientJob.class);
    Result added = service()
        .addClientJob("1", json);
    assertThat(added,
        is(new Result(HTTP_CREATED, TEXT_HTML, 2L)));
  }

  @Test
  public void testGetOutputs() {
    Result result = service().getOutputs("1");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON,
        singletonList(new OutputRec(1L, 1L, output.unloaded(), 0L)))));
  }

  @Test
  public void testGetNonExistentOutputData() {
    assertNotFound(service().getOutputData("0"),
        "Output ID not found: 0.");
  }

  @Test
  public void tesGetOutputData() {
    Result found = service()
        .getOutputData("1");
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON, "DATA")));
  }

  @Test
  public void testGetHdrData() {
    Result found = service()
        .getHdrData("1");
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON, HDR_LOG)));
  }

  @Test
  public void testGetAggregateHdrData() {
    Result found = service()
        .getAggregateHdrData("1", "GET");
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON, HDR_LOG)));
  }

  @Test
  public void testGetComparativeHdrData() {
    Result found = service()
        .getComparativeHdrData("1-2", "GET");

    Map<Long, HdrData> runs = new LinkedHashMap<>();
    runs.put(1L, HDR_LOG);
    runs.put(2L, HDR_LOG);

    Map<Pair, Double> pvalues = singletonMap(new Pair(1L, 2L), 0.0);

    Comparison expected = new Comparison(runs, pvalues);
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON, expected)));
  }

  @Test
  public void testGetComparativeHdrDataForDuplicateRun() {
    Result found = service()
        .getComparativeHdrData("1-1", "GET");
    Comparison expected = new Comparison(
        singletonMap(1L, HDR_LOG),
        singletonMap(new Pair(1L, 1L), 0.0)
    );
    assertThat(found,
        is(new Result(HTTP_OK, APPLICATION_JSON, expected)));
  }

  @Test
  public void testAddOutput() {
    OperationOutput zipped = OperationOutput.builder()
        .operation("GET")
        .data("data")
        .build();
    String json = gson.toJson(zipped, OperationOutput.class);
    Result added = service()
        .addOutput("1", json);
    assertThat(added,
        is(new Result(HTTP_CREATED, TEXT_HTML, 2L)));
  }

  @Test
  public void testGetNonExistentStatsLog() {
    assertNotFound(service().getStatsLog("0"),
        "VM stats log ID not found: 0.");
  }

  @Test
  public void testGetExistingStatsLog() throws IOException {
    CompressionService compressionService = compressionService(LZ4);

    byte[] data = { 1, 1, 1, 1 };
    Payload compressedData = compressionService
        .compress(data);
    StatsLog compressedLog = StatsLog.builder()
        .host("localhost")
        .payload(compressedData)
        .build();

    Store store = mock(Store.class);
    when(store.getStatsLog(1L))
        .thenReturn(of(new StatsRec(1L, 1L, compressedLog, 0L)));

    StoreService perfService = new StoreService(store, mock(HistogramService.class));

    Result found = perfService.getStatsLog("1");
    assertThat(found,
        is(new Result(HTTP_OK, TEXT_PLAIN, toUtfString(data))));
  }

  @Test
  public void testAddStatsLog() {
    String json = gson.toJson(log, StatsLog.class);
    Result added = service()
        .addStatsLog("1", json);
    assertThat(added, is(new Result(HTTP_CREATED, TEXT_HTML, 2L)));
  }

  @Test
  public void testGetOperationsForRun() {
    Result result = service().getOperationsForRun("1");
    assertThat(result, is(new Result(HTTP_OK, APPLICATION_JSON, singleton("GET"))));
  }

  @Test
  public void testGetCommonOperationsForRuns() {
    assertThat(
        service().getCommonOperationsForRuns("1-2"),
        is(new Result(HTTP_OK, APPLICATION_JSON, singleton("GET")))
    );

    assertThat(
        service().getCommonOperationsForRuns("2-3"),
        is(new Result(HTTP_OK, APPLICATION_JSON, singleton("MISS")))
    );

    assertThat(
        service().getCommonOperationsForRuns("1-2-3"),
        is(new Result(HTTP_OK, APPLICATION_JSON, emptySet()))
    );

    assertThat(
        service().getCommonOperationsForRuns("1-3"),
        is(new Result(HTTP_OK, APPLICATION_JSON, emptySet()))
    );
  }
}
