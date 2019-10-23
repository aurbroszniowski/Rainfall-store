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

import io.rainfall.store.data.Payload;
import io.rainfall.store.dataset.JobRecord;
import io.rainfall.store.dataset.OutputLogDataset;
import io.rainfall.store.dataset.OutputLogRecord;
import io.rainfall.store.dataset.RunDataset;
import io.rainfall.store.hdr.HdrData;
import io.rainfall.store.hdr.HistogramService;
import io.rainfall.store.values.ChangeReport;
import io.rainfall.store.values.Comparison;
import io.rainfall.store.values.OutputLog;
import io.rainfall.store.values.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@SuppressWarnings("unused")
public class OutputLogController extends ChildController<
    OutputLog, OutputLogRecord, JobRecord, OutputLogDataset> {

  private final HistogramService histogramService = new HistogramService();

  @Autowired
  private RunDataset runDataset;

  @Autowired
  private Gson gson;

  OutputLogController(OutputLogDataset dataset) {
    super(dataset, "Output log", "/outputs");
  }

  @PostMapping("/outputs/{jobId}")
  public ResponseEntity<?> postRun(@PathVariable long jobId, @RequestBody OutputLog outputLog) {
    return super.post(jobId, outputLog);
  }

  @GetMapping("/jobs/{parentId}/outputs")
  @ResponseBody
  public List<OutputLogRecord> listOutputLogsByJobId(@PathVariable long parentId) {
    return dataset()
        .findOutputLogsByRunId(parentId);
  }

  @GetMapping(value = "/outputs/{id}/hdr", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public String getOutputHdrData(@PathVariable long id) {
    Payload payload = getRecord(id)
        .getPayloadRecord()
        .getValue();
    byte[] data = uncompress(payload);
    HdrData hdrData = hdrData(data);
    return toJson(hdrData);
  }

  @GetMapping(value = "/runs/{runId}/aggregate/{operation}", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public String getAggregateHdrData(@PathVariable long runId,
                                    @PathVariable String operation) {
    HdrData hdrData = getHdrData(runId, operation);
    return toJson(hdrData);
  }

  private HdrData getHdrData(long runId, String operation) {
    List<Payload> records = dataset()
        .findOutputLogsByRunIdAndOperation(runId, operation);
    List<Supplier<InputStream>> suppliers = records.stream()
        .map(this::uncompress)
        .map(this::streamSupplier)
        .collect(toList());
    return histogramService.aggregateHdrData(suppliers);
  }

  @GetMapping(value = "/compare/{sids}/{operation}", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public String getComparativeHdrData(@PathVariable String sids,
                                      @PathVariable String operation) {
    long[] ids = parseIds(sids);
    Map<Long, HdrData> runs = LongStream.of(ids)
        .boxed()
        .collect(toMap(
            Function.identity(),
            id -> getHdrData(id, operation),
            (id1, id2) -> id1,
            LinkedHashMap::new
        ));
    Map<Pair, Double> pvalues = IntStream.range(0, ids.length)
        .boxed()
        .flatMap(i -> IntStream.range(i + 1, ids.length)
            .mapToObj(j -> new Pair(ids[i], ids[j]))
        )
        .collect(toMap(Function.identity(), pair -> {
          HdrData idata = runs.get(pair.getLeft());
          HdrData jdata = runs.get(pair.getRight());
          return histogramService.comparePercentiles(idata, jdata);
        }));
    Comparison comparison = new Comparison(runs, pvalues);
    return toJson(comparison);
  }

  private byte[] uncompress(Payload payload) {
    try {
      return compressionService(payload.getFormat())
          .decompress(payload);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Supplier<InputStream> streamSupplier(byte[] bytes) {
    return () -> new ByteArrayInputStream(bytes);
  }

  private HdrData hdrData(byte[] data) {
    return histogramService.readHdrData(() -> new ByteArrayInputStream(data));
  }

  @GetMapping(value = "/runs/{runId}/operations", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public List<String> listOperationsByRunId(@PathVariable long runId) {
    return dataset().findOperationsByRunId(runId);
  }

  @GetMapping(value = "/runs/{sids}/common-operations", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public Set<String> listCommonOperationsForRunsURL(@PathVariable String sids) {
    long[] runIds = parseIds(sids);
    return dataset().findCommonOperationsForRuns(runIds);
  }


  @GetMapping(path = "/runs/{runId}/regression/{threshold}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public String checkRegression(@PathVariable long runId,
                                @PathVariable float threshold) {
    long parentId = runDataset.getRecord(runId)
        .orElseThrow(() -> new IllegalArgumentException("Run not found for id: " + runId))
        .getParent()
        .getId();
    ChangeReport regressionCheck = runDataset.getLastBaselineID(parentId)
        .map(baselineID -> getChangeReport(baselineID, runId, threshold))
        .orElseGet(() -> new ChangeReport(threshold));
    return toJson(regressionCheck);
  }

  private ChangeReport getChangeReport(Long baselineID, long runId, double threshold) {
    List<String> operations = dataset().findOperationsByRunId(runId);
    Map<String, Double> pvalues = operations.stream()
        .collect(toMap(Function.identity(), op ->
            compareHdrToBaseline(baselineID, runId, op)));
    Map<String, Double> belowThreshold = pvalues.entrySet()
        .stream()
        .filter(e -> e.getValue() < threshold)
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new ChangeReport(baselineID, threshold, belowThreshold);
  }

  private double compareHdrToBaseline(long baselineID, long runID, String op) {
    HdrData baselineData = getHdrData(baselineID, op);
    HdrData hdrData = getHdrData(runID, op);
    return histogramService.comparePercentiles(baselineData, hdrData);
  }

  /**
   * Use Gson where Jackson fails
   */
  private String toJson(Object o) {
    return gson.toJson(o);
  }
}
