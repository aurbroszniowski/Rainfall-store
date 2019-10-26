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

import io.rainfall.store.core.ChangeReport;
import io.rainfall.store.core.ClientJob;
import io.rainfall.store.core.FileOutput;
import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.core.StatsLog;
import io.rainfall.store.core.TestRun;
import io.rainfall.store.data.CompressionService;
import io.rainfall.store.data.Payload;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.rainfall.store.client.DefaultStoreClientService.getPreviousSuccessfulCommit;
import static io.rainfall.store.core.TestRun.Status.COMPLETE;
import static io.rainfall.store.data.CompressionFormat.RAW;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SameParameterValue")
public abstract class AbstractStoreClientServiceTest {

  private static final URL OUTPUTS_URL = AbstractStoreClientServiceTest.class
      .getResource("/outputs");

  private static final List<String> DETAILS = asList(
      "Scenario : Data Access Phase",
      "Step 1)",
      "Operation weight : 100 %",
      "GetOperation"
  );
  private static final String VERSION = "1.1.1.1";
  private static final String CLASS_NAME = "MyClass";
  private static final ClientJob CLIENT_JOB = ClientJob.builder()
      .host("localhost")
      .clientNumber(1)
      .symbolicName("localhost-1")
      .details(String.join("\n", DETAILS))
      .build();

  private StoreClientService service;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private final CompressionService compressionService = compressionService(RAW);

  @Before
  public void setUp() throws Exception {
    service = createService();
  }

  abstract StoreClientService createService() throws Exception;

  @Test
  public void testAddRunToNonExistentTest() {
    thrown.expect(RuntimeException.class);
    service.addRun("NoSuchTest", CLASS_NAME, VERSION);
  }

  @Test
  public void testAddRunToExistingTest() {
    addTestCase("Test1");
    long runId = service.addRun("Test1", CLASS_NAME, VERSION);
    assertThat(runId, is(1L));

    TestRun expectedRun = TestRun.builder()
        .checksum(getPreviousSuccessfulCommit())
        .className(CLASS_NAME)
        .version(VERSION)
        .build();
    checkRuns("Test1", expectedRun);
  }

  @Test
  public void testSetStatusOfNonExistentRun() {
    boolean success = service.setStatus(1L, COMPLETE);
    assertFalse(success);
  }

  @Test
  public void testSetStatus() {
    addTestCase("Test1");
    long runId = service.addRun("Test1", CLASS_NAME, VERSION);
    boolean success = service.setStatus(runId, COMPLETE);
    assertTrue(success);
    TestRun expectedRun = TestRun.builder()
        .checksum(getPreviousSuccessfulCommit())
        .className(CLASS_NAME)
        .version(VERSION)
        .status(COMPLETE)
        .build();
    checkRuns("Test1", expectedRun);
  }

  abstract void addTestCase(String caseName);

  abstract void checkRuns(String caseName, TestRun expectedRun);

  abstract void checkClientJobs(ClientJob expectedClientJob);

  private OperationOutput getOutput(String path, String operation) {
    Path filePath = Paths.get(path, operation + ".hlog");
    try {
      byte[] data = Files.readAllBytes(filePath);
      Payload payload = compressionService.compress(data);
      return OperationOutput.builder()
          .operation(operation)
          .payload(payload)
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  abstract void checkOutputs(List<OperationOutput> expectedOutputs);

  void matchOutputs(List<OperationOutput> expectedOutputs, List<OperationOutput> outputs) {
    outputs.stream()
        .collect(Collectors.toMap(String::valueOf, Function.identity()))
        .forEach((s, output) -> {
              OperationOutput expected = expectedOutputs.stream()
                  .filter(o -> o.toString().equals(s))
                  .findFirst()
                  .orElseThrow(() -> new AssertionError(
                      "OperationOutput not matched: " + s));
              try {
                matchOutputs(expected, output);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
        );
  }

  @Test
  public void testCheckRegressionWithNoBaseline() {
    addTestCase("Test1");
    long runId = service.addRun("Test1", CLASS_NAME, VERSION);
    ChangeReport changeReport = service.checkRegression(runId, 0.0);
    assertThat(changeReport, is(new ChangeReport(0.0)));
  }

  abstract void checkChangeReport(ChangeReport changeReport, double threshold);

  abstract void setBaseline(long baselineId);

  abstract void checkLogs(StatsLog expectedLog) throws IOException;

  void matchOutputs(FileOutput expected, FileOutput actual) throws IOException {
    assertThat(actual.toString(), is(expected.toString()));
    assertThat(decompress(actual),
        is(decompress(expected)));
  }

  private byte[] decompress(FileOutput output) throws IOException {
    Payload payload = output.getPayload();
    return compressionService.decompress(payload);
  }
}
