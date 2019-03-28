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
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.Rec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.TestCaseRec;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.terracottatech.store.StoreException;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.configuration.MemoryUnit;
import com.terracottatech.store.manager.DatasetManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.terracottatech.store.manager.EmbeddedDatasetManagerBuilder.FileMode.NEW;
import static com.terracottatech.store.manager.EmbeddedDatasetManagerBuilder.FileMode.REOPEN;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class TcStorePersistenceTest {

  private static final Path PATH = new File("./performance").toPath();

  private static final TestCase testCase = TestCase.builder()
      .description("description")
      .build();

  private static final TestRun run = TestRun.builder()
      .version("v1")
      .className("TestClass")
      .checksum("00000")
      .build();

  private final ClientJob job = ClientJob.builder()
      .clientNumber(1)
      .host("localhost")
      .symbolicName("localhost-1")
      .details("details")
      .build();

  private static OperationOutput output1;
  private static OperationOutput output2;

  @BeforeClass
  public static void read() {
    output1 = OperationOutput.builder()
        .operation("MISS")
        .data("miss")
        .build();
    output2 = OperationOutput.builder()
        .operation("GET")
        .data("get")
        .build();
  }

  @Before
  public void write() throws StoreException {
    try (
        DatasetManager manager = DatasetManager.embedded()
            .offheap("offheap", 32, MemoryUnit.MB)
            .disk("disk", PATH, NEW)
            .build();
        RainfallStore store = new RainfallStore(manager, disk(manager))
    ) {
      store.addTestCase("Test1", testCase);
      long runId = store.addRun("Test1", run);
      long jobId = store.addClientJob(runId, job);
      store.addOutput(jobId, output1);
      store.addOutput(jobId, output2);
    }
  }

  private DatasetConfiguration disk(DatasetManager datasetManager) {
    return datasetManager.datasetConfiguration()
        .offheap("offheap")
        .disk("disk")
        .build();
  }

  @Test
  public void testReopenAndRead() throws StoreException {
    try (DatasetManager manager = reopened();
         RainfallStore store = new RainfallStore(manager, offheap(manager))
    ) {
      List<TestCaseRec> caseRecs = store.getTestCases();
      List<TestCase> cases = caseRecs
          .stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(cases, containsInAnyOrder(testCase));

      String caseId = caseRecs.stream()
          .map(Rec::getID)
          .findFirst()
          .orElse(null);

      List<RunRec> runRecs = store.getRuns(caseId);
      List<TestRun> runs = runRecs.stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(runs, contains(run));

      long runId = runRecs.stream()
          .mapToLong(Rec::getID)
          .findFirst()
          .orElse(-1L);

      List<OperationOutput> outputs = store.getOutputs(runId)
          .stream()
          .map(Rec::getValue)
          .collect(toList());
      assertThat(outputs, containsInAnyOrder(output1.unloaded(), output2.unloaded()));
    }
  }

  @Test
  public void testReopenAndWrite() throws StoreException {
    try (DatasetManager manager = reopened();
         RainfallStore store = new RainfallStore(manager, offheap(manager))
    ) {
      store.addTestCase("Test2", TestCase.builder()
          .description("description2")
          .build());
      List<String> caseIds = store.getTestCases()
          .stream()
          .map(Rec::getID)
          .collect(toList());
      assertThat(caseIds, containsInAnyOrder("Test1", "Test2"));

      long runId = store.addRun("Test1", run);
      assertThat(runId, is(2L));
      List<Long> runIds = caseIds.stream()
          .map(store::getRuns)
          .flatMap(List::stream)
          .map(Rec::getID)
          .collect(toList());
      assertThat(runIds, containsInAnyOrder(1L, 2L));

      long jobId = store.addClientJob(runId, job);
      assertThat(jobId, is(2L));
      List<Long> jobIds = runIds.stream()
          .map(store::getClientJobs)
          .flatMap(List::stream)
          .map(Rec::getID)
          .collect(toList());
      assertThat(jobIds, containsInAnyOrder(1L, 2L));

      MatcherAssert.assertThat(store.addOutput(jobId, output1), is(3L));
      MatcherAssert.assertThat(store.addOutput(jobId, output2), is(4L));
      List<Long> outputIds = jobIds.stream()
          .map(store::getOutputs)
          .flatMap(List::stream)
          .map(Rec::getID)
          .collect(toList());
      assertThat(outputIds, containsInAnyOrder(1L, 2L, 3L, 4L));
    }
  }

  @Test
  public void testReopenAndCreateIndex() throws StoreException {
    try (DatasetManager manager = reopened(64);
         RainfallStore store = new RainfallStore(manager, offheap(manager))
    ) {
      store.indexParents();
      Set<String> operations = store.getOperationsForRun(1L);
      assertThat(operations, containsInAnyOrder("GET", "MISS"));
    }
  }

  @Test
  public void testReopenAndRecreateIndex() throws StoreException {
    try (DatasetManager manager = reopened(64);
         RainfallStore store = new RainfallStore(manager, offheap(manager))
             .indexParents()
    ) {
      store.indexParents();
      Set<String> operations = store.getOperationsForRun(1L);
      assertThat(operations, containsInAnyOrder("GET", "MISS"));
    }
  }

  private DatasetManager reopened() throws StoreException {
    return reopened(32);
  }

  private DatasetManager reopened(int offheapSize) throws StoreException {
    return DatasetManager.embedded()
        .offheap("offheap", offheapSize, MemoryUnit.MB)
        .disk("disk", PATH, REOPEN)
        .build();
  }

  private DatasetConfiguration offheap(DatasetManager manager) {
    return manager.datasetConfiguration()
        .offheap("offheap")
        .build();
  }

  @After
  public void cleanUp() throws IOException {
    Files.walk(PATH)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .map(File::delete)
        .forEach(Assert::assertTrue);
    assertThat(PATH.toFile().exists(), is(false));
  }
}
