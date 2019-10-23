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
import io.rainfall.store.dataset.CaseRecord;
import io.rainfall.store.dataset.Record;
import io.rainfall.store.values.Case;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.SEE_OTHER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseControllerIT extends ControllerIT {

  @Autowired
  private CaseDataset caseDataset;

  private final Case testCase = Case.builder()
      .name("Test1")
      .description("Some test")
      .build();

  @Test
  public void testRoot() throws Exception {
    testGetCases("/");
  }

  @Test
  public void testGetCases() throws Exception {
    testGetCases("/cases");
  }

  private void testGetCases(String url) throws Exception {
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(containsAll(
            "Performance tests",
            "Name",
            "Description",
            "Created",
            "Updated"
        )));
  }

  @Transactional
  @Test
  public void testGetCase() throws Exception {
    long caseId = caseDataset.save(testCase).getId();
    mvc.perform(get("/cases/" + caseId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(containsAll(
            "Test1",
            "Some test"
        )));
  }

  @Transactional
  @Test
  public void testPostCase() throws Exception {
    String caseName = testCase.getName();
    RequestBuilder post = post("/cases")
        .param("name", caseName)
        .param("description", testCase.getDescription());
    HttpServletResponse response = mvc.perform(post)
        .andReturn()
        .getResponse();
    assertThat(
        response.getStatus(),
        is(SEE_OTHER.value())
    );
    assertThat(
        response.getHeader("Location"),
        endsWith("/cases/" + caseName)
    );
    Iterable<CaseRecord> records = caseDataset.getRecords();
    Set<Case> all = stream(records.spliterator(), false)
        .map(Record::getValue)
        .collect(toSet());
    assertThat(all, contains(testCase));
  }
}
