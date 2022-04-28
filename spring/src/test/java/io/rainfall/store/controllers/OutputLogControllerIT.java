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

package io.rainfall.store.controllers;

import io.rainfall.store.Utils;
import io.rainfall.store.data.Payload;
import io.rainfall.store.dataset.CaseDataset;
import io.rainfall.store.dataset.JobDataset;
import io.rainfall.store.dataset.OutputLogDataset;
import io.rainfall.store.dataset.OutputLogRecord;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.hdr.HdrData;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.ChangeReport;
import io.rainfall.store.values.Job;
import io.rainfall.store.values.OutputLog;
import io.rainfall.store.values.Run;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.rainfall.store.Utils.readBytes;
import static io.rainfall.store.Utils.toLz4CompressedOutput;
import static io.rainfall.store.hdr.Percentile.MAX;
import static io.rainfall.store.hdr.Percentile.MEDIAN;
import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
public class OutputLogControllerIT extends ControllerIT {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private JobDataset jobDataset;

  @Autowired
  private OutputLogDataset outputLogDataset;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private Gson gson;

  private long runId;
  private long jobId;
  private long outputId;
  private OutputLog outputLog;

  @Before
  public void setupAndCreateLog() throws Exception {
    super.setup();

    Case testCase = Case.builder()
        .name("Test1")
        .description("Some test")
        .build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder()
        .status(INCOMPLETE)
        .version("1.1.1.1")
        .baseline(false)
        .className("my.Class")
        .build();
    runId = runDataset.save(caseId, run).getId();

    Job job = Job.builder()
        .clientNumber(1)
        .host("localhost")
        .symbolicName("localhost-1")
        .details("details")
        .build();
    jobId = jobDataset.save(runId, job)
        .getId();

    Payload payload = readBytes("153.hlog");
    outputLog = OutputLog.builder()
        .operation("MISS")
        .format("hlog")
        .payload(payload)
        .build();
    outputId = outputLogDataset.save(jobId, outputLog).getId();
  }

  @Transactional
  @Test
  public void testPostOutputLog() throws Exception {
    String json = objectMapper.writeValueAsString(outputLog);
    RequestBuilder post = post("/outputs/" + jobId)
        .contentType(APPLICATION_JSON_UTF8)
        .content(json);
    MockHttpServletResponse response = mvc.perform(post)
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse();
    long outputId = Long.valueOf(response.getContentAsString());
    assertThat(
        response.getHeader("Location"),
        matchesPattern(".*/outputs/" + outputId)
    );
    OutputLog saved = outputLogDataset.getRecord(outputId)
        .map(Record::getValue)
        .orElseThrow(AssertionError::new);
    assertThat(saved.getFormat(), is(outputLog.getFormat()));
    assertThat(saved.getOperation(), is(outputLog.getOperation()));
  }

  @Transactional
  @Test
  public void testListOutputLogsByJobId() throws Exception {
    String url = format("/jobs/%d/outputs", jobId);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    List<OutputLogRecord> records = objectMapper
        .readValue(json, new TypeReference<List<OutputLogRecord>>() {
        });
    Set<OutputLog> runs = records.stream()
        .map(Record::getValue)
        .collect(toSet());
    assertThat(runs, contains(outputLog));
  }

  @Transactional
  @Test
  public void testGetOutputHdrData() throws Exception {
    String url = format("/outputs/%d/hdr", outputId);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    HdrData hdrData = fromJson(json);
    Stream.of(hdrData.getStartTimes(), hdrData.getTps(),
        hdrData.getMeans(), hdrData.getErrors())
        .map(List::size)
        .forEach(size -> MatcherAssert.assertThat(size, lessThanOrEqualTo(200)));
    MatcherAssert.assertThat(hdrData.getValueAtPercentile(MAX), is(34701311L));
    MatcherAssert.assertThat(hdrData.getPercentilePoints().size(), is(126));
    MatcherAssert.assertThat(hdrData.getPercentileValues().size(), is(126));
  }

  @Transactional
  @Test
  public void testListOperationsByRunId() throws Exception {
    String url = format("/runs/%d/operations", runId);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    List<String> operations = objectMapper
        .readValue(json, new TypeReference<List<String>>() {
        });
    assertThat(operations, contains(outputLog.getOperation()));
  }

  @Transactional
  @Test
  public void testGetAggregateHdrData() throws Exception {
    Stream.of("105.hlog", "106.hlog", "109.hlog", "111.hlog")
        .map(Utils::toLz4CompressedGetOutput)
        .forEach(outputLog -> outputLogDataset.save(jobId, outputLog));
    String url = format("/runs/%d/aggregate/%s",
        runId, "GET");
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    HdrData hdrData = fromJson(json);
    Stream.of(hdrData.getStartTimes(), hdrData.getTps(),
        hdrData.getMeans(), hdrData.getErrors(),
        hdrData.getTimedPercentiles(MEDIAN))
        .map(List::size)
        .forEach(size -> MatcherAssert.assertThat(size, is(59)));
    MatcherAssert.assertThat(hdrData.getValueAtPercentile(MAX), is(0L));
  }

  @Transactional
  @Test
  public void testListCommonOperationsForRunsURL() throws Exception {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    Job job = Job.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    outputLogDataset.save(jobId1, forOp("GET"));
    outputLogDataset.save(jobId1, forOp("MISS"));

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    outputLogDataset.save(jobId2, forOp("GET"));

    String url = format("/runs/%d-%d/common-operations", runId1, runId2);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    List<String> operations = objectMapper
        .readValue(json, new TypeReference<List<String>>() {
        });
    assertThat(operations, contains("GET"));
  }

  @Transactional
  @Test
  public void testGetComparativeHdrData() throws Exception {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    Job job = Job.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    Stream.of("105.hlog", "106.hlog")
        .map(Utils::toLz4CompressedGetOutput)
        .forEach(outputLog -> outputLogDataset.save(jobId1, outputLog));

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    Stream.of("109.hlog", "111.hlog")
        .map(Utils::toLz4CompressedGetOutput)
        .forEach(outputLog -> outputLogDataset.save(jobId2, outputLog));

    String url = format("/compare/%d-%d/%s",
        runId1, runId2, "GET");
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
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
    assertThat(runs.keySet(), contains(runId1, runId2));

    @SuppressWarnings("unchecked")
    Map<String, Double> pvalues = (Map<String, Double>)comparison.get("pvalues");
    MatcherAssert.assertThat(pvalues.size(), is(1));
    List<Long> pair = gson.fromJson(
        pvalues.keySet().iterator().next(),
        new TypeToken<List<Long>>() {
        }.getType()
    );
    assertThat(pair, is(asList(runId1, runId2)));

    double pvalue = pvalues.values().iterator().next();
    assertThat(pvalue, is(0.4175236528177705));
  }

  @Transactional
  @Test
  public void testCheckRegressionWithoutBaseline() throws Exception {
    ChangeReport changeReport = getChangeReport(runId, 0.0f);
    assertThat(changeReport, is(new ChangeReport(0.0)));
  }

  @Transactional
  @Test
  public void testCheckRegressionWithNonExistentRun() throws Exception {
    String url = format("/runs/%d/regression/%f", 200L, 0.0f);
    expectErrorPage(get(url), url);
  }

  @Transactional
  @Test
  public void testCheckRegressionDifferentOperations() throws Exception {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    Job job = Job.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    OutputLog getLog = toLz4CompressedOutput("105.hlog", "GET");
    outputLogDataset.save(jobId1, getLog);

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    OutputLog missLog = toLz4CompressedOutput("105.hlog", "MISS");
    outputLogDataset.save(jobId2, missLog);

    runDataset.setBaseline(runId1, true);

    ChangeReport changeReport = getChangeReport(runId2, 0.5f);
    assertThat(changeReport, is(new ChangeReport(runId1, 0.5f,
        singletonMap("MISS", 0.4175236528177705))));
  }

  @Transactional
  @Test
  public void testCheckRegressionChangeDetected() throws Exception {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    Job job = Job.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    OutputLog outputLog1 = toLz4CompressedOutput("105.hlog", "GET");
    outputLogDataset.save(jobId1, outputLog1);

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    OutputLog outputLog2 = toLz4CompressedOutput("153.hlog", "GET");
    outputLogDataset.save(jobId2, outputLog2);

    runDataset.setBaseline(runId1, true);

    ChangeReport changeReport = getChangeReport(runId2, 0.1f);
    assertThat(changeReport, is(new ChangeReport(runId1, 0.1f,
        singletonMap("GET", 0.0))));
  }

  @Transactional
  @Test
  public void testCheckRegressionNoChangeDetected() throws Exception {
    Case testCase = Case.builder().build();
    long caseId = caseDataset.save(testCase).getId();

    Run run = Run.builder().build();
    Job job = Job.builder().build();

    long runId1 = runDataset.save(caseId, run).getId();
    long jobId1 = jobDataset.save(runId1, job).getId();
    outputLogDataset.save(jobId1, outputLog);

    long runId2 = runDataset.save(caseId, run).getId();
    long jobId2 = jobDataset.save(runId2, job).getId();
    outputLogDataset.save(jobId2, outputLog);

    runDataset.setBaseline(runId1, true);

    ChangeReport changeReport = getChangeReport(runId2, 0.1f);
    assertThat(changeReport, is(new ChangeReport(runId1, 0.1f,
        emptyMap())));
  }

  private ChangeReport getChangeReport(long runId, float threshold) throws Exception {
    String url = format("/runs/%d/regression/%f", runId, threshold);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return gson.fromJson(json, ChangeReport.class);
  }

  private OutputLog forOp(String op) {
    return OutputLog.builder()
        .operation(op)
        .format("hlog")
        .payload(Payload.raw("xxx"))
        .build();
  }

  /**
   * Use Gson where Jackson fails.
   */
  private HdrData fromJson(String json) {
    return gson.fromJson(
        json,
        HdrData.class
    );
  }
}
