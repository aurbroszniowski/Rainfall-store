package io.rainfall.store.controllers;

import io.rainfall.store.dataset.CaseDataset;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.dataset.RunRecord;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.Run;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static io.rainfall.store.values.Run.Status.COMPLETE;
import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
public class RunControllerIT extends ControllerIT {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private ObjectMapper objectMapper;

  private final String caseName = "Test1";

  private final Run run = Run.builder()
      .status(INCOMPLETE)
      .version("1.1.1.1")
      .baseline(false)
      .className("my.Class")
      .build();

  private long caseId;
  private long runId;

  @Before
  @Override
  public void setup() {
    super.setup();
    Case testCase = Case.builder()
        .name(caseName)
        .description("Some test")
        .build();
    caseId = caseDataset.save(testCase).getId();
    runId = runDataset.save(caseId, run).getId();
  }

  @Transactional
  @Test
  public void testPostRun() throws Exception {
    String json = objectMapper.writeValueAsString(run);
    RequestBuilder post = post("/runs/" + caseName)
        .contentType(APPLICATION_JSON_UTF8)
        .content(json);
    MockHttpServletResponse response = mvc.perform(post)
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse();
    long runId = Long.valueOf(response.getContentAsString());
    assertThat(
        response.getHeader("Location"),
        matchesPattern(".*/runs/" + runId)
    );
    Run saved = runDataset.getRecord(runId)
        .map(Record::getValue)
        .orElse(null);
    assertThat(saved, is(run));
  }

  @Transactional
  @Test
  public void testGetRunsByCaseID() throws Exception {
    String url = format("/cases/%d/runs", caseId);
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(matchRun()));
  }

  @Transactional
  @Test
  public void testListRunsByCaseID() throws Exception {
    String url = format("/cases/%d/runs/json", caseId);
    String json = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andReturn()
        .getResponse()
        .getContentAsString();

    List<RunRecord> records = objectMapper
        .readValue(json, new TypeReference<List<RunRecord>>() {
        });
    Set<Run> runs = records.stream()
        .map(Record::getValue)
        .collect(toSet());
    assertThat(runs, contains(run));
  }

  @Transactional
  @Test
  public void testGetRun() throws Exception {
    mvc.perform(get("/runs/" + runId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(matchRunWithParent())
        );
  }

  @Transactional
  @Test
  public void testSetBaseline() throws Exception {
    String url = format("/runs/%d/baseline", runId);
    String booleanValue = objectMapper.writeValueAsString(true);
    RequestBuilder post = post(url)
        .contentType(APPLICATION_JSON_UTF8)
        .content(booleanValue);
    mvc.perform(post)
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8));
    boolean baseline = runDataset.getRecord(runId)
        .map(Record::getValue)
        .map(Run::isBaseline)
        .orElse(false);
    assertTrue(baseline);
  }

  @Transactional
  @Test
  public void testSetStatus() throws Exception {
    String url = format("/runs/%d/status", runId);
    RequestBuilder post = post(url)
        .contentType(DEFAULT_TEXT_HTML)
        .content(COMPLETE.name());
    mvc.perform(post)
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_UTF8));
    Run.Status status = runDataset.getRecord(runId)
        .map(Record::getValue)
        .map(Run::getStatus)
        .orElse(null);
    assertThat(status, is(COMPLETE));
  }

  @Transactional
  @Test
  public void testGetCompareReport() throws Exception {
    long runId1 = runDataset.save(caseId, run).getId();
    long runId2 = runDataset.save(caseId, run).getId();
    String url = format("/compare/%d-%d", runId1, runId2);
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(matchRunWithParent()));
  }

  private Matcher<String> matchRunWithParent() {
    return allOf(
        containsString("Test1"),
        matchRun());
  }

  private Matcher<String> matchRun() {
    return containsAll(
        run.getVersion(),
        run.getClassName(),
        run.getChecksum(),
        run.getStatus().name()
    );
  }
}
