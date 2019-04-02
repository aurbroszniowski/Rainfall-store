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

package io.rainfall.store.service;

import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.FileOutput;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.data.CompressionService;
import io.rainfall.store.data.Payload;
import io.rainfall.store.record.ClientJobRec;
import io.rainfall.store.record.DuplicateNameException;
import io.rainfall.store.record.OutputRec;
import io.rainfall.store.record.Rec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.StatsRec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.TestCaseRec;
import io.rainfall.store.service.hdr.HdrData;
import io.rainfall.store.service.hdr.HistogramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;

public class StoreService {

  static final String NAME_REGEX = "[A-Za-z0-9_-]+";

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreService.class);

  private static final Comparator<Rec> TIMESTAMP_CMP = comparing(Rec::getTimeStamp);

  private static final Comparator<ClientJobRec> JOB_CMP = comparing(Rec::getValue,
      comparing(ClientJob::getClientNumber));

  private final Store store;
  private final Gson gson = new Gson();
  private final HistogramService histogramService;

  public StoreService(Store store) {
    this(store, new HistogramService());
  }

  StoreService(Store store, HistogramService histogramService) {
    this.store = store;
    this.histogramService = histogramService;
  }

  public List<TestCaseRec> listTestCases() {
    return store.getTestCases()
        .stream()
        .sorted(TIMESTAMP_CMP)
        .collect(toList());
  }

  public Result getTestCase(String uniqueName) {
    return store.getTestCase(uniqueName)
        .map(this::found)
        .orElseGet(() -> notFound(uniqueName, "Test"));
  }

  public Result getRuns(String uniqueName) {
    List<RunRec> recs = store.getRuns(uniqueName)
        .stream()
        .sorted(TIMESTAMP_CMP.reversed())
        .collect(toList());
    return new Result(HTTP_OK, APPLICATION_JSON, recs);
  }

  public Result addTestCase(String uniqueName, String description) {
    return (uniqueName.matches(NAME_REGEX))
        ? tryAdd(uniqueName, description)
        : reportInvalidName(uniqueName);
  }

  private Result tryAdd(String uniqueName, String description) {
    TestCase testCase = TestCase.builder()
        .description(description)
        .build();
    try {
      store.addTestCase(uniqueName, testCase);
      LOGGER.info("TestCase created: ID={}.", uniqueName);
      return new Result(HTTP_CREATED, TEXT_HTML, uniqueName);
    } catch (DuplicateNameException e) {
      LOGGER.warn("Attempt to add a TestCase with an existing name: '{}'.",
          uniqueName);
      return new Result(HTTP_CONFLICT, TEXT_HTML, e.getMessage());
    }
  }

  private Result reportInvalidName(String uniqueName) {
    LOGGER.warn("Attempt to add a TestCase with an invalid name: '{}'.", uniqueName);
    String msg = String.format("Invalid TestCase name: '%s'; must match '%s'.",
        uniqueName, NAME_REGEX);
    return new Result(HTTP_BAD_REQUEST, TEXT_HTML, msg);
  }

  public Result getRun(String sid) {
    return store.getRun(Long.valueOf(sid))
        .map(this::found)
        .orElseGet(() -> notFound(sid, "Run"));
  }

  public Result getClientJobs(String sid) {
    List<ClientJobRec> recs = store.getClientJobs(Long.valueOf(sid))
        .stream()
        .sorted(JOB_CMP)
        .collect(toList());
    return new Result(HTTP_OK, APPLICATION_JSON, recs);
  }

  public Result getStats(String sid) {
    List<StatsRec> recs = store.getStats(Long.valueOf(sid));
    return new Result(HTTP_OK, APPLICATION_JSON, recs);
  }

  public Result getStats(String sid, String host) {
    List<StatsRec> recs = store.getStats(Long.valueOf(sid), host);
    return new Result(HTTP_OK, APPLICATION_JSON, recs);
  }

  public Result addRun(String caseName, String body) {
    return add(caseName, body, store::addRun, TestRun.class);
  }

  public Result setStatus(String sid, String statusBody) {
    try {
      long runID = Long.valueOf(sid);
      String statusName = gson.fromJson(statusBody, String.class);
      TestRun.Status status = TestRun.Status.valueOf(statusName);
      boolean success = store.setStatus(runID, status);
      if (success) {
        LOGGER.info("Status of run {} set to {}.", runID, status);
      } else {
        LOGGER.error("Run ID not found: {}.", runID);
      }
      int code = success ? HTTP_OK : HTTP_CONFLICT;
      return new Result(code, TEXT_HTML, success);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to set status of run {} to {}: {}.", sid, statusBody, e.getMessage());
      throw e;
    }
  }

  public Result setBaseline(String sid, String statusBody) {
    try {
      long runID = Long.valueOf(sid);
      boolean value = gson.fromJson(statusBody, Boolean.class);
      boolean success = store.setBaseline(runID, value);
      if (success) {
        LOGGER.info("Baseline status of run {} set to {}.", runID, value);
      } else {
        LOGGER.error("Run ID not found: {}.", runID);
      }
      int code = success ? HTTP_OK : HTTP_CONFLICT;
      return new Result(code, TEXT_HTML, success);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to set baseline status of run {} to {}: {}.",
          sid, statusBody, e.getMessage());
      throw e;
    }
  }

  public Result getClientJob(String sid) {
    return store.getClientJob(Long.valueOf(sid))
        .map(this::found)
        .orElseGet(() -> notFound(sid, "Client job"));
  }

  public Result addClientJob(String runId, String body) {
    return add(runId, body, store::addClientJob, ClientJob.class);
  }

  public Result getOutputs(String sid) {
    List<OutputRec> recs = store.getOutputs(Long.valueOf(sid));
    return new Result(HTTP_OK, APPLICATION_JSON, recs);
  }

  public Result getOutputData(String sid) {
    return getOutputView(sid, String::new);
  }

  private Result getOutputView(String sid, Function<byte[], Object> view) {
    try {
      long id = Long.valueOf(sid);
      return store.getOutput(id)
          .map(Rec::getValue)
          .map(FileOutput::getPayload)
          .map(this::uncompress)
          .map(view)
          .map(this::found)
          .orElseGet(() -> notFound(sid, "Output"));
    } catch (Throwable e) {
      LOGGER.error("Output could not be retrieved for ID={}: {}.",
          sid, e.getMessage());
      throw e;
    }
  }

  public Result getHdrData(String sid) {
    return getOutputView(sid, this::hdrData);
  }

  private HdrData hdrData(byte[] data) {
    return histogramService.readHdrData(() -> new ByteArrayInputStream(data));
  }

  private byte[] uncompress(Payload payload) {
    try {
      CompressionService compressionService = compressionService(payload.getFormat());
      return compressionService.decompress(payload);
    } catch (IOException e) {
      LOGGER.error("Failed to uncompressed data for operation output.");
      throw new IllegalArgumentException(e);
    }
  }

  public Result getAggregateHdrData(String sid, String operation) {
    try {
      Long runId = Long.valueOf(sid);
      HdrData hdrData = getHdrData(runId, operation);
      return found(hdrData);
    } catch (RuntimeException e) {
      LOGGER.error("Error generating aggregate report for {}/{}: {}.",
          sid, operation, e.getMessage());
      return new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
          singletonMap("msg", e.getMessage()));
    }
  }

  private HdrData getHdrData(long runId, String operation) {
    List<Supplier<InputStream>> inputStreams = store.getOutputsForOperation(runId, operation)
        .stream()
        .peek(rec -> LOGGER.info("Aggregating output log: {}.", rec.getID()))
        .map(Rec::getValue)
        .map(FileOutput::getPayload)
        .map(this::uncompress)
        .map(this::streamSupplier)
        .collect(toList());
    return histogramService.aggregateHdrData(inputStreams);
  }

  private Supplier<InputStream> streamSupplier(byte[] bytes) {
    return () -> new ByteArrayInputStream(bytes);
  }

  public Result getComparativeHdrData(String sids, String operation) {
    try {
      long[] ids = Stream.of(sids.split("-"))
          .mapToLong(Long::valueOf)
          .toArray();
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
      return found(comparison);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to get {} HDR data for runs {}: {}.",
          operation, sids, e.getMessage());
      return new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
          singletonMap("msg", "Failed to get " + operation + " HDR data for runs " + sids + "."));
    }
  }

  public Result checkRegression(String sid, String sThreshold) {
    try {
      long runID = Long.valueOf(sid);
      double threshold = Double.valueOf(sThreshold);
      String testName = store.getRun(runID)
          .orElseThrow(() -> new IllegalArgumentException("Run ID not found: " + runID))
          .getParentID();
      ChangeReport regressionCheck = store.getLastBaselineID(testName)
          .map(baselineID -> getChangeReport(baselineID, runID, threshold))
          .orElseGet(() -> new ChangeReport(threshold));
      return new Result(HTTP_OK, APPLICATION_JSON, regressionCheck);
    } catch (RuntimeException e) {
      LOGGER.error("Error reporting regression for run {}: {}.",
          sid, e.getMessage());
      return new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
          singletonMap("msg", e.getMessage()));
    }
  }

  private ChangeReport getChangeReport(Long baselineID, long runID, double threshold) {
    Set<String> operations = store.getOperationsForRun(runID);
    Map<String, Double> pvalues = operations.stream()
        .collect(toMap(Function.identity(), op ->
            compareHdrToBaseline(baselineID, runID, op)));
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

  public Result addOutput(String jobId, String body) {
    return add(jobId, body, store::addOutput, OperationOutput.class);
  }

  public Result getStatsLog(String sid) {
    try {
      long id = Long.valueOf(sid);
      return store.getStatsLog(id)
          .map(Rec::getValue)
          .map(FileOutput::getPayload)
          .map(this::uncompress)
          .map(Payload::toUtfString)
          .map(data -> new Result(HTTP_OK, TEXT_PLAIN, data))
          .orElseGet(() -> notFound(id, "VM stats log"));
    } catch (Throwable e) {
      LOGGER.error("VM stats log could not be retrieved for ID={}: {}.",
          sid, e.getMessage());
      throw e;
    }
  }

  public Result addStatsLog(String runId, String body) {
    return add(runId, body, store::addStatsLog, StatsLog.class);
  }

  public Result getOperationsForRun(String sid) {
    try {
      Set<String> operations = store.getOperationsForRun(Long.valueOf(sid));
      return new Result(HTTP_OK, APPLICATION_JSON, operations);
    } catch (Throwable e) {
      LOGGER.error("Outputs could not be retrieved for ID={}: {}.",
          sid, e.getMessage());
      throw e;
    }
  }

  public Result getCommonOperationsForRuns(String sids) {
    try {
      Set<String> ops = Stream.of(sids.split("-"))
          .map(Long::valueOf)
          .map(store::getOperationsForRun)
          .reduce(this::intersection)
          .orElseGet(Collections::emptySet);
      return new Result(HTTP_OK, APPLICATION_JSON, ops);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to get common operations for runs {}: {}.", sids, e.getMessage());
      throw e;
    }
  }

  private Set<String> intersection(Set<String> s1, Set<String> s2) {
    return s1.stream().filter(s2::contains).collect(Collectors.toSet());
  }

  public Result compareRuns(String sids) {
    try {
      List<RunRec> recs = Stream.of(sids.split("-"))
          .map(Long::valueOf)
          .map(store::getRun)
          .map(o -> o.orElseThrow(IllegalArgumentException::new))
          .collect(toList());
      return new Result(HTTP_OK, APPLICATION_JSON, recs);
    } catch (IllegalArgumentException e) {
      return new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
          singletonMap("msg", "Run IDs not found: " + sids + "."));
    }
  }

  private Result found(Object object) {
    return new Result(HTTP_OK, APPLICATION_JSON, object);
  }

  private Result notFound(Object id, String msgStart) {
    return new Result(HTTP_NOT_FOUND, APPLICATION_JSON,
        singletonMap("msg", msgStart + " ID not found: " + id + "."));
  }

  private <V> Result add(String parentId, String body, BiFunction<Long, V, Long> adder, Class<V> type) {
    return add(Long.valueOf(parentId), body, adder, type);
  }

  private <P, V> Result add(P parentId, String body, BiFunction<P, V, Long> adder, Class<V> type) {
    String simpleName = type.getSimpleName();
    try {
      V value = gson.fromJson(body, type);
      long id = adder.apply(parentId, value);
      LOGGER.info("{} created: ID={}, parent ID={}.", simpleName, id, parentId);
      return new Result(HTTP_CREATED, TEXT_HTML, id);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to add {}, parent ID={}: {}.", simpleName, parentId, e.getMessage());
      throw e;
    }
  }
}

