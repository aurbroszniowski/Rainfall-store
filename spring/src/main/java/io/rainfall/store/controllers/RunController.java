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

import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.dataset.RunRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class RunController {

  private final RunDataset dataset;

  @Autowired
  RunController(RunDataset dataset) {
    this.dataset = dataset;
  }

  @GetMapping({ "/cases/{parentId}/runs" })
  public ModelAndView getRunsByCaseID(ModelMap model, @PathVariable long parentId) {
    List<RunRecord> runs = dataset.findByParentId(parentId);
    model.addAttribute("runs", runs);
    return new ModelAndView("runs", model);
  }

  @GetMapping({ "/cases/{parentId}/runs/json" })
  @ResponseBody
  public List<RunRecord> listRunsByCaseID(@PathVariable long parentId) {
    return dataset.findByParentId(parentId);
  }

  @GetMapping({ "/runs/{id}" })
  public ModelAndView getRun(ModelMap model, @PathVariable long id) {
    model.addAttribute("run", getRecord(id));
    return new ModelAndView("run", model);
  }

  @PostMapping("/runs/{id}/baseline")
  public ResponseEntity<?> setBaseline(@PathVariable long id,
                                       @RequestBody String booleanBody) {
    //bug in jackson?
    boolean baseline = Boolean.valueOf(
        booleanBody.replaceAll("=$", ""));
    dataset.setBaseline(id, baseline);
    return ResponseEntity.ok(baseline);
  }

  private RunRecord getRecord(Long id) {
    return dataset.getRecord(id)
        .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
  }
}
