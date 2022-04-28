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

package io.rainfall.store.service.spark;

import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.data.CompressionService;
import io.rainfall.store.hdr.HdrData;
import io.rainfall.store.record.OutputRec;
import io.rainfall.store.record.Rec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.tc.RainfallStore;
import io.rainfall.store.service.Result;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.configuration.MemoryUnit;
import com.terracottatech.store.manager.DatasetManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.terracottatech.store.manager.DatasetManager.embedded;
import static io.rainfall.store.core.TestRun.Status.COMPLETE;
import static io.rainfall.store.data.CompressionFormat.LZ4;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static io.rainfall.store.data.Payload.toUtfString;
import static io.rainfall.store.hdr.Percentile.MAX;
import static io.rainfall.store.hdr.Percentile.MEDIAN;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.jetty.http.MimeTypes.Type;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML_UTF_8;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertTrue;

public class StoreControllerTest {

  private static final int PORT = 14566;
  private static final String PATH = "performance";
  private static final String URL = format("http://localhost:%d/%s/", PORT, PATH);

  private static final Map<String, Type> TYPE_MAP = Stream.of(Type.values())
      .collect(toMap(Type::asString, identity()));

  private static final Gson gson = new Gson();

  private static final TestCase testCase = TestCase.builder()
      .description("description")
      .build();

  private static final TestRun run = TestRun.builder()
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

  private static final OperationOutput output1 = OperationOutput.builder()
      .data("DATA")
      .operation("MISS")
      .build();

  private static final OperationOutput output2 = OperationOutput.builder()
      .data("LOG")
      .operation("GET")
      .build();

  private static final StatsLog log = StatsLog.builder()
      .host("localhost")
      .data("1111")
      .build();

  private Store store;
  private StoreController controller;

  @Rule
  public TestName name = new TestName();

  @Before
  public void setUp() throws StoreException {
    store = store();
    controller = new StoreController(store, PATH, PORT)
        .awaitInitialization();
  }

  private Store store() throws StoreException {
    String resourceName = getClass().getSimpleName() + "." + name.getMethodName();
    DatasetManager datasetManager = embedded()
        .offheap(resourceName, 20, MemoryUnit.MB)
        .build();
    DatasetConfiguration config = datasetManager.datasetConfiguration()
        .offheap(resourceName)
        .build();
    return new RainfallStore(datasetManager, config);
  }

  @After
  public void close() throws Exception {
    controller.close();
    store.close();
  }

  @Test
  public void testGetTestCases() {
    Stream.of("", "/", "cases")
        .forEach(path -> assertPageContains(
            get(path), "Performance tests"));
  }

  @Test
  public void testGetNonExistentTestCase() {
    assertPageContains(get("cases/NoSuchName"),
        "Test ID not found: NoSuchName.");
  }

  @Test
  public void testGetExistingTestCase() {
    store.addTestCase("Test1", testCase);
    Result result = get("cases/Test1");
    assertPageContains(result, "Test1");
  }

  private void assertPageContains(Result result, String msg) {
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(TEXT_HTML_UTF_8));
    assertThat(result.getContent().toString(),
        containsString(msg));
  }

  /**
   * Creation of a test redirects to the new test's page.
   */
  @Test
  public void testAddTestCase() {
    Result created = submit("cases", "Test1", "description");
    Result fetched = get("cases/Test1");
    assertThat(created, is(fetched));
    assertPageContains(fetched, "Test1");
  }

  @Test
  public void testAddTestCaseWithDuplicateName() {
    submit("cases", "Test1", "description");
    Result failure = submit("cases", "Test1", "otherDescription");
    assertThat(failure,
        is(new Result(HTTP_CONFLICT, TEXT_HTML, "Unique name already exists: Test1.")));
  }

  @Test
  public void testGetNonExistentRun() {
    assertPageContains(get("runs/0"),
        "Run ID not found: 0.");
  }

  @Test
  public void testGetRuns() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertPageContains(get("cases/Test1/runs"), "TestClass");
  }

  @Test
  public void testGetRunsList() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    Result result = get("cases/Test1/runs/json");

    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));
    List<RunRec> recs = gson.fromJson(
        result.getContent().toString(),
        new TypeToken<List<RunRec>>() {
        }.getType()
    );
    assertThat(
        recs.stream().map(Rec::getID).collect(toList()),
        contains(runId)
    );
    assertThat(
        recs.stream().map(Rec::getValue).collect(toList()),
        contains(run)
    );
  }

  @Test
  public void testGetExistingRun() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertPageContains(get("runs/1"), "TestClass");
  }

  @Test
  public void testAddRunToNonExistentTestCase() {
    Result post = post("runs/Test1", run);
    assertThat(post.getCode(), is(HTTP_INTERNAL_ERROR));
    assertPageContains(get("runs/1"),
        "Run ID not found: 1.");
  }

  @Test
  public void testAddRun() {
    store.addTestCase("Test1", testCase);
    assertThat(post("runs/Test1", run),
        is(new Result(HTTP_CREATED, TEXT_HTML, "1")));
    assertPageContains(get("runs/1"), "TestClass");
  }

  @Test
  public void testSetStatusOfNonExistentRun() {
    Result post = post("runs/1/status", "COMPLETE");
    assertThat(post,
        is(new Result(HTTP_CONFLICT, TEXT_HTML, Boolean.toString(false))));
  }

  @Test
  public void testSetStatus() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertThat(post("runs/1/status", "COMPLETE"),
        is(new Result(HTTP_OK, TEXT_HTML, Boolean.toString(true))));
    TestRun.Status status = store.getRun(1L)
        .orElseThrow(IllegalStateException::new)
        .getValue()
        .getStatus();
    assertThat(status, is(COMPLETE));
  }

  @Test
  public void testSetNonExistentRunAsBaseline() {
    Result post = post("runs/1/baseline", "true");
    assertThat(post,
        is(new Result(HTTP_CONFLICT, TEXT_HTML, Boolean.toString(false))));
  }

  @Test
  public void testSetBaseline() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertThat(post("runs/1/baseline", "true"),
        is(new Result(HTTP_OK, TEXT_HTML, Boolean.toString(true))));
    boolean baseline = store.getRun(1L)
        .orElseThrow(IllegalStateException::new)
        .getValue()
        .isBaseline();
    assertTrue(baseline);
  }

  @Test
  public void testCompareRuns() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    store.addRun("Test1", run);

    store.addTestCase("Test2", testCase);
    store.addRun("Test2", run);

    Result comparisonReport = get("compare/1-2-3");
    assertThat(comparisonReport.getCode(), is(HTTP_OK));
    assertThat(comparisonReport.getContentType(), is(TEXT_HTML_UTF_8));
    String body = comparisonReport.getContent().toString();
    assertThat(body, containsString("Test1"));
    assertThat(body, containsString("Test2"));
  }

  @Test
  public void testCheckRegressionWithoutBaseline() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);

    Result result = get("runs/1/regression/0.1");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    ChangeReport regressionCheck = gson.fromJson(
        result.getContent().toString(),
        ChangeReport.class
    );
    assertThat(regressionCheck, is(new ChangeReport(0.1)));
  }

  @Test
  public void testCheckRegression() {
    store.addTestCase("Test1", testCase);

    long baselineId = store.addRun("Test1", run);
    addOutput(
        store.addClientJob(baselineId, job),
        "105.hlog"
    );
    store.setBaseline(baselineId, true);

    long runId = store.addRun("Test1", run);
    addOutput(
        store.addClientJob(runId, job),
        "106.hlog"
    );

    Result result = get("runs/" + runId + "/regression/0.5");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    ChangeReport changeReport = gson.fromJson(
        result.getContent().toString(),
        ChangeReport.class
    );
    assertThat(changeReport.getBaselineID(),
        is(Optional.of(baselineId)));
    assertThat(changeReport.getThreshold(),
        is(0.5));
    assertThat(changeReport.getPValues(),
        is(singletonMap("GET", 0.4175236528177705)));
  }


  @Test
  public void testGetNonExistentJob() {
    assertPageContains(get("jobs/0"),
        "Client job ID not found: 0.");
  }

  private void assertErrorPageContains(Result result, String msg) {
    assertThat(result.getCode(), is(HTTP_NOT_FOUND));
    assertThat(result.getContentType(), is(TEXT_HTML));
    assertThat(result.getContent().toString(),
        containsString(msg));
  }

  @Test
  public void testGetClientJobs() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addClientJob(runId, job);
    assertPageContains(get("runs/1/jobs"), "localhost");
  }

  @Test
  public void testGetStats() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addStatsLog(runId, log);
    assertPageContains(get("runs/1/stats"), "localhost");
  }

  @Test
  public void testGetStatsForRunAndHost() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addStatsLog(runId, log);
    assertPageContains(get("runs/1/stats/localhost"), "localhost");
  }

  @Test
  public void testGetExistingJob() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addClientJob(runId, job);
    assertPageContains(get("jobs/1"), "localhost-1");
  }

  @Test
  public void testAddJob() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertThat(post("jobs/1", job),
        is(new Result(HTTP_CREATED, TEXT_HTML, "1")));
    assertPageContains(get("jobs/1"), "localhost-1");
  }

  @Test
  public void testGetOutputs() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    long id1 = store.addOutput(jobId, output1);
    long id2 = store.addOutput(jobId, output2);
    Result result = get("jobs/1/outputs");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));
    List<OutputRec> recs = gson.fromJson(
        result.getContent().toString(),
        new TypeToken<List<OutputRec>>() {
        }.getType()
    );
    assertThat(
        recs.stream().map(Rec::getID).collect(toList()),
        containsInAnyOrder(id1, id2)
    );
    assertThat(
        recs.stream().map(Rec::getValue).collect(toList()),
        containsInAnyOrder(output1.unloaded(), output2.unloaded())
    );
  }

  @Test
  public void testGetNonExistentOutput() {
    assertErrorPageContains(get("outputs/0"),
        "Output ID not found: 0.");
  }

  @Test
  public void testGetExistingOutputs() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    store.addOutput(jobId, output1);
    store.addOutput(jobId, output2);
    assertData(get("outputs/1"), "DATA");
    assertData(get("outputs/2"), "LOG");
  }

  @Test
  public void testAddOutput() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addClientJob(runId, job);
    OperationOutput output = OperationOutput.builder()
        .operation("GET")
        .data("data")
        .build();
    assertThat(post("outputs/1", output),
        is(new Result(HTTP_CREATED, TEXT_HTML, "1")));
    assertData(get("outputs/1"), "data");
  }

  @Test
  public void testGetHdrData() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    addOutput(jobId, "153.hlog");

    Result result = get("outputs/1/io.rainfall.store.service.spark");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    HdrData hdrData = gson.fromJson(
        result.getContent().toString(),
        HdrData.class
    );
    Stream.of(hdrData.getStartTimes(), hdrData.getTps(),
        hdrData.getMeans(), hdrData.getErrors())
        .map(List::size)
        .forEach(size -> assertThat(size, lessThanOrEqualTo(200)));
    assertThat(hdrData.getValueAtPercentile(MAX), is(34701311L));
    assertThat(hdrData.getPercentilePoints().size(), is(126));
    assertThat(hdrData.getPercentileValues().size(), is(126));
  }

  private long addOutput(long jobId, String resourceName) {
    try {
      String path = StoreControllerTest.class
          .getResource(resourceName)
          .getPath();
      byte[] bytes = readAllBytes(Paths.get(path));
      CompressionService compressionService = compressionService(LZ4);
      OperationOutput output = OperationOutput.builder()
          .operation("GET")
          .payload(compressionService.compress(bytes))
          .build();
      return store.addOutput(jobId, output);
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  @Test
  public void testGetAggregateHdrData() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    Stream.of("105.hlog", "106.hlog", "109.hlog", "111.hlog")
        .forEach(name -> addOutput(jobId, name));

    Result result = get("runs/1/aggregate/GET");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    HdrData hdrData = gson.fromJson(
        result.getContent().toString(),
        HdrData.class
    );
    Stream.of(hdrData.getStartTimes(), hdrData.getTps(),
        hdrData.getMeans(), hdrData.getErrors(),
        hdrData.getTimedPercentiles(MEDIAN))
        .map(List::size)
        .forEach(size -> assertThat(size, is(59)));
    assertThat(hdrData.getValueAtPercentile(MAX), is(0L));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetComparativeHdrData() {
    store.addTestCase("Test1", testCase);

    long runId1 = store.addRun("Test1", run);
    long jobId1 = store.addClientJob(runId1, job);
    Stream.of("105.hlog", "106.hlog")
        .forEach(name -> addOutput(jobId1, name));

    long runId2 = store.addRun("Test1", run);
    long jobId2 = store.addClientJob(runId2, job);
    Stream.of("109.hlog", "111.hlog")
        .forEach(name -> addOutput(jobId2, name));

    Result result = get("/compare/1-2/GET");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    String json = result.getContent().toString();
    Map<String, ?> comparison = gson.fromJson(
        json,
        new TypeToken<Map<String, ?>>() {
        }.getType()
    );

    Map<Long, HdrData> runs = gson.fromJson(
        comparison.get("runs").toString(),
        new TypeToken<Map<Long, HdrData>>() {
        }.getType()
    );
    assertThat(runs.keySet(), contains(1L, 2L));
    runs.values().forEach(hdrData ->
        assertThat(hdrData.getValueAtPercentile(MAX), is(0L))
    );

    Map<String, Double> pvalues = (Map<String, Double>)comparison.get("pvalues");
    assertThat(pvalues.size(), is(1));

    List<Long> pair = gson.fromJson(
        pvalues.keySet().iterator().next(),
        new TypeToken<List<Long>>() {
        }.getType()
    );
    assertThat(pair, is(Arrays.asList(1L, 2L)));

    double pvalue = pvalues.values().iterator().next();
    assertThat(pvalue, is(0.4175236528177705));
  }

  @Test
  public void testGetNonExistentStatsLog() {
    assertErrorPageContains(get("stats/0"),
        "VM stats log ID not found: 0.");
  }

  @Test
  public void testGetExistingStatsLog() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    store.addStatsLog(runId, log);
    assertPlainText(get("stats/1"), log);
  }

  @Test
  public void testAddStatsLog() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    assertThat(post("stats/1", log),
        is(new Result(HTTP_CREATED, TEXT_HTML, "1")));
    assertPlainText(get("stats/1"), log);
  }

  @Test
  public void testGetOperationsForRun() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    store.addOutput(jobId, output1);
    store.addOutput(jobId, output2);

    Result result = get("runs/1/operations");
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));

    Set<String> operations = gson.fromJson(
        result.getContent().toString(),
        new TypeToken<Set<String>>() {
        }.getType()
    );
    assertThat(operations, containsInAnyOrder("GET", "MISS"));
  }

  @Test
  public void testGetCommonOperationsForRuns() {
    store.addTestCase("Test1", testCase);

    long run1 = store.addRun("Test1", run);
    long jobId1 = store.addClientJob(run1, job);
    store.addOutput(jobId1, output1);

    long run2 = store.addRun("Test1", run);
    long jobId2 = store.addClientJob(run2, job);
    store.addOutput(jobId2, output2);

    assertThat(operations(get("runs/1-2/common-operations")),
        empty());

    store.addTestCase("Test2", testCase);
    long run3 = store.addRun("Test2", run);
    long jobId3 = store.addClientJob(run3, job);
    store.addOutput(jobId3, output1);
    store.addOutput(jobId3, output2);
    assertThat(operations(get("runs/1-3/common-operations")),
        containsInAnyOrder("MISS"));
    assertThat(operations(get("runs/2-3/common-operations")),
        containsInAnyOrder("GET"));
    assertThat(operations(get("runs/3/common-operations")),
        containsInAnyOrder("GET", "MISS"));
    assertThat(operations(get("runs/1-2-3/common-operations")),
        empty());
  }

  private Set<String> operations(Result result) {
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(APPLICATION_JSON));
    return gson.fromJson(
        result.getContent().toString(),
        new TypeToken<Set<String>>() {
        }.getType()
    );
  }

  @SuppressWarnings("SameParameterValue")
  private Result submit(String path, String uniqueName, String description) {
    Client client = new ResteasyClientBuilderImpl()
        .build();
    try {
      Form form = new Form("name", uniqueName)
          .param("description", description);
      Entity<Form> entity = Entity.form(form);
      Response response = client.target(URL)
          .path(path)
          .request()
          .post(entity);
      return follow(response);
    } finally {
      client.close();
    }
  }

  private Result redirect(Response response) {
    String location = response.getHeaderString("Location")
        .replaceAll("^/" + PATH, "");
    return get(location);
  }

  private Result post(String path, Object content) {
    Client client = new ResteasyClientBuilderImpl()
        .build();
    try {
      String json = gson.toJson(content);
      Entity<String> text = Entity.text(json);
      Response response = client.target(URL)
          .path(path)
          .request()
          .post(text);
      return result(response);
    } finally {
      client.close();
    }
  }

  private Result get(String path) {
    Client client = new ResteasyClientBuilderImpl()
        .build();
    try {
      Response response = client.target(URL)
          .path(path)
          .request()
          .get();
      return follow(response);
    } finally {
      client.close();
    }
  }

  private Result follow(Response response) {
    switch (response.getStatus()) {
      case HTTP_SEE_OTHER:
        return redirect(response);
      default:
        return result(response);
    }
  }

  private Result result(Response response) {
    int responseCode = response.getStatus();
    MediaType mediaType = response.getMediaType();
    Type contentType = TYPE_MAP.get(mediaType.toString());
    String responseContent = response.readEntity(String.class);
    return new Result(responseCode, contentType, responseContent);
  }

  @SuppressWarnings("SameParameterValue")
  private void assertPlainText(Result result, StatsLog output) {
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(TEXT_PLAIN));
    assertThat(
        result.getContent().toString(),
        is(toUtfString(output.getPayload().getData()))
    );
  }

  private void assertData(Result result, String data) {
    assertThat(result.getCode(), is(HTTP_OK));
    assertThat(result.getContentType(), is(TEXT_PLAIN));
    Object content = result.getContent();
    assertThat(content, is(data));
  }
}
