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

package io.rainfall.store.client;

import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.data.Payload;
import io.rainfall.store.record.Rec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.StoreWriter;
import io.rainfall.store.record.tc.RainfallStore;
import io.rainfall.store.service.spark.StoreController;
import spark.Spark;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.terracottatech.store.StoreException;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.configuration.MemoryUnit;
import com.terracottatech.store.manager.DatasetManager;

import static com.terracottatech.store.manager.DatasetManager.embedded;
import static io.rainfall.store.core.TestRun.Status.COMPLETE;
import static io.rainfall.store.data.CompressionFormat.RAW;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static java.lang.String.format;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public abstract class AbstractStoreClientTest {

  private static final int PORT = 14568;
  private static final String PATH = "qa";
  private static final String URL = format("http://localhost:%d/%s", PORT, PATH);

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

  private final StoreWriter client = client(URL);

  private Store store;
  private StoreController controller;

  @Before
  public void setUp() throws StoreException {
    store = store();
    controller = new StoreController(store, PATH, PORT)
        .awaitInitialization();
  }

  @After
  public void close() throws Exception {
    controller.close();
    store.close();
  }

  @Rule
  public TestName name = new TestName();

  private Store store() throws StoreException {
    String resourceName = getClass().getSimpleName() + "." + name.getMethodName();
    DatasetManager datasetManager = embedded()
        .offheap(resourceName, 20, MemoryUnit.MB)
        .build();
    DatasetConfiguration config = datasetManager.datasetConfiguration()
        .offheap(resourceName)
        .build();
    return new RainfallStore(datasetManager, config);
  }

  protected abstract StoreWriter client(String url);

  @Test(expected = UnsupportedOperationException.class)
  public void testAddTestCase() {
    client.addTestCase("Test1", testCase);
    fail();
  }

  @Test(expected = RuntimeException.class)
  public void testAddRunToNonExistentTestCase() {
    client.addRun("Test1", run);
    fail();
  }

  @Test
  public void testAddRun() {
    store.addTestCase("Test1", testCase);

    long ID = client.addRun("Test1", run);
    assertThat(ID, is(1L));

    TestRun added = store.getRun(ID)
        .map(Rec::getValue)
        .orElse(null);
    assertThat(added, is(run));
  }

  @Test
  public void testSetStatusOfNonExistentRun() {
    boolean success = client.setStatus(1L, COMPLETE);
    assertFalse(success);
  }

  @Test
  public void testSetStatus() {
    store.addTestCase("Test1", testCase);
    store.addRun("Test1", run);
    boolean success = client.setStatus(1L, COMPLETE);
    assertTrue(success);
  }

  @Test
  public void testAddJob() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);

    long ID = client.addClientJob(runId, job);
    assertThat(ID, is(1L));

    ClientJob added = store.getClientJob(ID)
        .map(Rec::getValue)
        .orElse(null);
    assertThat(added, is(job));
  }

  @Test
  public void testAddOutput() throws Exception {
    String file = AbstractStoreClientTest.class
        .getResource("/outputs/1_scenario/GET.hlog")
        .getFile();
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    long jobId = store.addClientJob(runId, job);
    byte[] data = readAllBytes(get(file));
    Payload payload = compressionService(RAW).compress(data);
    OperationOutput output = OperationOutput.builder()
        .operation("GET")
        .payload(payload)
        .build();
    long ID = client.addOutput(jobId, output);
    assertThat(ID, is(1L));

    OperationOutput added = store.getOutput(ID)
        .map(Rec::getValue)
        .orElse(null);
    assertThat(added, is(output));
  }

  @Test
  public void testAddStatsLog() {
    store.addTestCase("Test1", testCase);
    long runId = store.addRun("Test1", run);
    StatsLog log = StatsLog.builder()
        .host("localhost")
        .data("1111")
        .build();

    long ID = client.addStatsLog(runId, log);
    assertThat(ID, is(1L));

    StatsLog added = store.getStatsLog(ID)
        .map(Rec::getValue)
        .orElse(null);
    assertThat(added, is(log));
  }

  @AfterClass
  public static void stop() {
    Spark.stop();
  }
}
