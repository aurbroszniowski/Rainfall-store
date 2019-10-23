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

import io.rainfall.store.dataset.JobDataset;
import io.rainfall.store.dataset.JobRecord;
import io.rainfall.store.dataset.RunRecord;
import io.rainfall.store.values.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SuppressWarnings("unused")
public class JobController extends ChildController<Job, JobRecord, RunRecord, JobDataset> {

  @Autowired
  JobController(JobDataset dataset) {
    super(dataset, "Job", "/jobs");
  }

  @PostMapping("/jobs/{runId}")
  public ResponseEntity<?> postJob(@PathVariable long runId, @RequestBody Job job) {
    return super.post(runId, job);
  }

  @GetMapping({ "/runs/{parentId}/jobs" })
  public ModelAndView getJobsByRunID(ModelMap model, @PathVariable long parentId) {
    return getByParentId(model, parentId);
  }

  @GetMapping({ "/jobs/{id}" })
  public ModelAndView getJob(ModelMap model, @PathVariable long id) {
    return get(model, id);
  }
}
