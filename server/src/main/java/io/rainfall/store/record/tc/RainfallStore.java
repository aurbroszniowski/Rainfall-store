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

package io.rainfall.store.record.tc;

import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.ClientJobRec;
import io.rainfall.store.record.OutputRec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.StatsRec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.TestCaseRec;

import com.terracottatech.store.Dataset;
import com.terracottatech.store.Record;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.manager.DatasetManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class RainfallStore implements Store {

  private final DatasetManager datasetManager;
  private final TestCaseDataset testCases;
  private final RunDataset runs;
  private final JobDataset jobs;
  private final OutputDataset outputs;
  private final StatsDataset stats;

  public RainfallStore(DatasetManager datasetManager, DatasetConfiguration config)
      throws StoreException {
    this.datasetManager = datasetManager;
    this.testCases = new TestCaseDataset(
        createDataset("testCases", config, Type.STRING));
    this.runs = new RunDataset(testCases,
        createDataset("runs", config));
    this.jobs = new JobDataset(runs,
        createDataset("jobs", config));
    this.outputs = new OutputDataset(jobs,
        createDataset("outputs", config));
    this.stats = new StatsDataset(runs,
        createDataset("stats", config));
  }

  private Dataset<Long> createDataset(String name, DatasetConfiguration config)
      throws StoreException {
    return createDataset(name, config, Type.LONG);
  }

  private <K extends Comparable<K>> Dataset<K> createDataset(
      String name, DatasetConfiguration config, Type<K> type)
      throws StoreException {
    datasetManager.newDataset(name, type, config);
    return datasetManager.getDataset(name, type);
  }

  @SuppressWarnings("unchecked")
  public RainfallStore indexParents() {
    Stream.of(runs, jobs, outputs, stats)
        .parallel()
        .forEach(ChildDataset::indexParent);
    return this;
  }

  @Override
  public void addTestCase(String uniqueName, TestCase testCase) {
    testCases.add(uniqueName, testCase);
  }

  @Override
  public Optional<TestCaseRec> getTestCase(String uniqueName) {
    return testCases.get(uniqueName);
  }

  @Override
  public List<TestCaseRec> getTestCases() {
    return testCases.list();
  }


  @Override
  public long addRun(String caseId, TestRun run) {
    return runs.add(caseId, run);
  }

  @Override
  public Optional<RunRec> getRun(long id) {
    return runs.get(id);
  }

  @Override
  public List<RunRec> getRuns(String caseName) {
    return runs.list(caseName);
  }


  @Override
  public long addClientJob(long runId, ClientJob job) {
    return jobs.add(runId, job);
  }

  @Override
  public Optional<ClientJobRec> getClientJob(long id) {
    return jobs.get(id);
  }

  @Override
  public List<ClientJobRec> getClientJobs(long runId) {
    return jobs.list(runId);
  }


  @Override
  public long addOutput(long jobId, OperationOutput output) {
    return outputs.add(jobId, output);
  }

  @Override
  public Optional<OutputRec> getOutput(long id) {
    return outputs.get(id);
  }

  @Override
  public List<OutputRec> getOutputs(long jobId) {
    return outputs.list(jobId);
  }

  @Override
  public long addStatsLog(long runId, StatsLog log) {
    return stats.add(runId, log);
  }

  @Override
  public boolean setStatus(long runId, TestRun.Status status) {
    return runs.setStatus(runId, status);
  }

  @Override
  public boolean setBaseline(long runId, boolean value) {
    return runs.setBaseline(runId, value);
  }

  @Override
  public Optional<StatsRec> getStatsLog(long id) {
    return stats.get(id);
  }

  @Override
  public List<StatsRec> getStats(long runId) {
    return stats.list(runId);
  }

  @Override
  public List<StatsRec> getStats(long runId, String host) {
    return stats.list(runId, host);
  }

  @Override
  public Optional<Long> getLastBaselineID(String testName) {
    return runs.getLastBaselineID(testName);
  }

  @Override
  public Set<String> getOperationsForRun(long runId) {
    return jobs.children(runId)
        .map(Record::getKey)
        .flatMap(outputs::getOperations)
        .collect(toSet());
  }

  @Override
  public List<OutputRec> getOutputsForOperation(long runId, String operation) {
    return jobs.children(runId)
        .map(Record::getKey)
        .flatMap(jobID -> outputs.getOutputsForOperation(jobID, operation))
        .collect(toList());
  }

  @Override
  public void close() {
    testCases.close();
    runs.close();
    outputs.close();
  }
}
