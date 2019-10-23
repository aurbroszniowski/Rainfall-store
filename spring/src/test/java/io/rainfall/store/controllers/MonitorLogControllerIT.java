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
import io.rainfall.store.dataset.MonitorLogDataset;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.MonitorLog;
import io.rainfall.store.values.Run;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
public class MonitorLogControllerIT extends ControllerIT {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private MonitorLogDataset logDataset;

  @Autowired
  private ObjectMapper objectMapper;

  private long runId;
  private long logId;
  private MonitorLog log;

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

    Payload payload = Utils.readBytes("150.hlog");
    log = MonitorLog.builder()
        .host("localhost")
        .type("vmstat")
        .payload(payload)
        .build();
    logId = logDataset.save(runId, log).getId();
  }

  @Transactional
  @Test
  public void testPostLog() throws Exception {
    String json = objectMapper.writeValueAsString(log);
    RequestBuilder post = post("/stats/" + runId)
        .contentType(APPLICATION_JSON_UTF8)
        .content(json);
    MockHttpServletResponse response = mvc.perform(post)
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse();
    long logId = Long.valueOf(response.getContentAsString());
    assertThat(
        response.getHeader("Location"),
        matchesPattern(".*/stats/" + logId)
    );
    MonitorLog saved = logDataset.getRecord(logId)
        .map(Record::getValue)
        .orElse(null);
    assertThat(saved, is(log));
  }

  @Transactional
  @Test
  public void testGetLog() throws Exception {
    mvc.perform(get("/stats/" + logId))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/plain;charset=UTF-8"));
  }

  @Transactional
  @Test
  public void testFindMonitorLogsForRun() throws Exception {
    String url = format("/runs/%d/stats", runId);
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(matchLog()));
  }

  @Transactional
  @Test
  public void testFindMonitorLogsForRunAndHost() throws Exception {
    String url = format("/runs/%d/stats/%s", runId, log.getHost());
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(matchLog()));
  }

  private Matcher<String> matchLog() {
    return containsAll(
        log.getHost(),
        log.getType()
    );
  }
}
