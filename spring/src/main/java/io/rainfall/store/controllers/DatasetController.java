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

import io.rainfall.store.dataset.Dataset;
import io.rainfall.store.dataset.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.stream.Stream;

abstract class DatasetController<R extends Record<?>, S extends Dataset<R, ?>> {

  protected final String path;
  private final S dataset;
  private final String valueName;

  @Autowired
  DatasetController(S dataset, String valueName, String path) {
    this.dataset = dataset;
    this.valueName = valueName;
    this.path = path;
  }

  ResponseEntity<?> post(long id, HttpStatus status) {
    URI location = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .path(path + "/{id}")
        .buildAndExpand(id)
        .toUri();
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setLocation(location);
    return new ResponseEntity<>(id, responseHeaders, status);
  }

  ModelAndView get(ModelMap model, long id) {
    String viewName = valueName.toLowerCase();
    model.addAttribute(viewName, getRecord(id));
    return new ModelAndView(viewName, model);
  }

  R getRecord(long id) {
    return dataset.getRecord(id)
        .orElseThrow(() -> new IllegalArgumentException(valueName + " not found: " + id));
  }

  S dataset() {
    return dataset;
  }

  long[] parseIds(String sids) {
    return Stream.of(sids.split("-"))
        .mapToLong(Long::valueOf)
        .toArray();
  }
}
