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
import io.rainfall.store.values.Case;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.http.HttpStatus.SEE_OTHER;

@Controller
@SuppressWarnings("unused")
class CaseController {

  private final CaseDataset dataset;

  @Autowired
  CaseController(CaseDataset dataset) {
    this.dataset = dataset;
  }

  @GetMapping({ "/", "/cases" })
  public ModelAndView getCases(ModelMap model) {
    model.addAttribute("cases", dataset.getRecords());
    return new ModelAndView("cases", model);
  }

  @GetMapping({ "/cases/{id}" })
  public ModelAndView getCase(ModelMap model, @PathVariable long id) {
    model.addAttribute("case", getRecord(id));
    return new ModelAndView("case", model);
  }

  @PostMapping({ "/cases" })
  public ResponseEntity<?> postCase(String name, String description) {
    Case value = Case.builder()
        .name(name)
        .description(description)
        .build();
    dataset.save(value);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{name}")
        .buildAndExpand(name)
        .toUri();
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setLocation(location);
    return new ResponseEntity<>(null, responseHeaders, SEE_OTHER);
  }

  private CaseRecord getRecord(Long id) {
    return dataset.getRecord(id)
        .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
  }
}
