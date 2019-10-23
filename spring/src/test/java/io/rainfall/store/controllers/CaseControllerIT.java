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

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseControllerIT extends ControllerIT {

  @SuppressWarnings("unused")
  @Autowired
  private CaseDataset caseDataset;

  private final Case testCase = Case.builder()
      .name("Test1")
      .description("Some test")
      .build();

  @Test
  public void testRoot() throws Exception {
    HttpServletResponse response = mvc.perform(get("/"))
        .andExpect(status().isFound())
        .andReturn()
        .getResponse();
    assertThat(
        response.getHeader("Location"),
        startsWith("/cases")
    );
  }

  @Test
  public void testGetCases() throws Exception {
    mvc.perform(get("/cases"))
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
        .andExpect(status().isSeeOther())
        .andReturn()
        .getResponse();
    assertThat(
        response.getHeader("Location"),
        matchesPattern(".*/cases/[0-9]+")
    );
    Iterable<CaseRecord> records = caseDataset.getRecords();
    Set<Case> all = stream(records.spliterator(), false)
        .map(Record::getValue)
        .collect(toSet());
    assertThat(all, contains(testCase));
  }

  @Transactional
  @Test
  public void testPostBlankName() throws Exception {
    RequestBuilder post = post("/cases")
        .param("name", "")
        .param("description", "Description");
    expectErrorPage(post, "/cases");
  }

  @Transactional
  @Test
  public void testPostTooLongName() throws Exception {
    String name = String.join("", nCopies(256, "n"));
    RequestBuilder post = post("/cases")
        .param("name", name)
        .param("description", "Description");
    expectErrorPage(post, "/cases");
  }

  @Transactional
  @Test
  public void testPostTooLongDescription() throws Exception {
    String description = String.join("", nCopies(1025, "n"));
    RequestBuilder post = post("/cases")
        .param("name", "name")
        .param("description", description);
    expectErrorPage(post, "/cases");
  }

  @Transactional
  @Test
  public void testPostNoName() throws Exception {
    RequestBuilder post = post("/cases")
        .param("description", "Description");
    expectErrorPage(post, "/cases");
  }

  @Transactional
  @Test
  public void testGetCompareForm() throws Exception {
    caseDataset.save(testCase);
    mvc.perform(get("/compare"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(DEFAULT_TEXT_HTML))
        .andExpect(content().string(containsAll(
            "Select test runs to compare",
            "Test1"
        )));
  }
}
