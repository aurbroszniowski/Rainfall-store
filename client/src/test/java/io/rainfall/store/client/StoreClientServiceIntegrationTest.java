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

import io.rainfall.store.client.resteasy.RestEasyStoreClient;
import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestCase;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.ClientJobRec;
import io.rainfall.store.record.OutputRec;
import io.rainfall.store.record.Rec;
import io.rainfall.store.record.RunRec;
import io.rainfall.store.record.StatsRec;
import io.rainfall.store.record.Store;
import io.rainfall.store.record.tc.RainfallStore;
import io.rainfall.store.service.spark.StoreController;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.terracottatech.store.StoreException;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.configuration.MemoryUnit;
import com.terracottatech.store.manager.DatasetManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.terracottatech.store.manager.DatasetManager.embedded;
import static io.rainfall.store.data.CompressionFormat.RAW;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StoreClientServiceIntegrationTest extends AbstractStoreClientServiceTest {

  private static final int PORT = 14569;
  private static final String PATH = "performance";
  private static final String URL = format("http://localhost:%d/%s", PORT, PATH);

  private Store store;
  private StoreController controller;

  @Rule
  public TestName name = new TestName();

  @Override
  StoreClientService createService() throws StoreException {
    String resourceName = getClass().getSimpleName() + "." + name.getMethodName();
    DatasetManager datasetManager = embedded()
        .offheap(resourceName, 20, MemoryUnit.MB)
        .build();
    DatasetConfiguration config = datasetManager.datasetConfiguration()
        .offheap(resourceName)
        .build();
    store = new RainfallStore(datasetManager, config);
    controller = new StoreController(store, PATH, PORT)
        .awaitInitialization();
    StoreClient client = new RestEasyStoreClient(URL);
    return new DefaultStoreClientService(client, compressionService(RAW));
  }

  @Override
  void addTestCase(String caseName) {
    TestCase testCase = TestCase.builder()
        .description("description")
        .build();
    store.addTestCase(caseName, testCase);
  }

  @Override
  void checkRuns(String caseName, TestRun expectedRun) {
    List<RunRec> runRecs = store.getRuns(caseName);
    assertThat(toIds(runRecs), contains(1L));
    assertThat(toValues(runRecs), contains(expectedRun));
  }

  @Override
  void checkClientJobs(ClientJob expectedClientJob) {
    List<ClientJobRec> jobRecs = store.getClientJobs(1L);
    assertThat(toIds(jobRecs), contains(1L));
    assertThat(toValues(jobRecs), contains(expectedClientJob));
  }

  @Override
  void checkOutputs(List<OperationOutput> expectedOutputs) {
    List<OutputRec> outputRecs = store.getOutputs(1L)
        .stream()
        .map(Rec::getID)
        .map(store::getOutput)
        .map(Optional::get)
        .collect(toList());
    assertThat(toIds(outputRecs), containsInAnyOrder(1L, 2L));
    matchOutputs(toValues(outputRecs), expectedOutputs);
  }

  @Override
  void checkChangeReport(ChangeReport changeReport, double threshold) {
    Map<String, Double> pValues = new HashMap<>();
    pValues.put("GET", 0.4175236528177705);
    assertThat(changeReport, is(new ChangeReport(1L, threshold, pValues)));
  }

  @Override
  void setBaseline(long baselineId) {
    store.setBaseline(baselineId, true);
  }

  @Override
  void checkLogs(StatsLog expectedLog) throws IOException {
    List<StatsRec> stats = store.getStats(1L);
    assertThat(toIds(stats), contains(1L));
    matchOutputs(toValues(stats).get(0), expectedLog);
  }

  private <K extends Comparable<K>> List<K> toIds(List<? extends Rec<K, ?>> recs) {
    return recs.stream()
        .map(Rec::getID)
        .collect(toList());
  }

  private <V> List<V> toValues(List<? extends Rec<?, V>> recs) {
    return recs.stream()
        .map(Rec::getValue)
        .collect(toList());
  }

  @After
  public void stopService() throws Exception {
    store.close();
    controller.close();
  }
}
