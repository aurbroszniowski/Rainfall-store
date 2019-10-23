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

import io.rainfall.store.dataset.CaseDataset;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.dataset.RunRecord;
import io.rainfall.store.values.Case;
import io.rainfall.store.values.Run;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static io.rainfall.store.values.Run.Status.INCOMPLETE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RunControllerIT extends ControllerIT {

  @Autowired
  private CaseDataset caseDataset;

  @Autowired
  private RunDataset runDataset;

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
        .name("Test1")
        .description("Some test")
        .build();
    caseId = caseDataset.save(testCase).getId();
    runId = runDataset.save(caseId, run).getId();
  }

  @Transactional
  @Test
  public void testGetRunsByCaseID() throws Exception {
    String url = format("/cases/%d/runs", caseId);
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(containsAll(
            run.getVersion(),
            run.getClassName(),
            run.getChecksum(),
            run.getStatus().name())
        ));
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

    List<RunRecord> records = new ObjectMapper()
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
        .andExpect(content().string(containsAll(
            "Test1",
            run.getVersion(),
            run.getClassName(),
            run.getChecksum(),
            run.getStatus().name())
        ));
  }
}
