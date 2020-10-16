/*
 * Copyright (c) 2014-2020 Aurélien Broszniowski
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
import io.rainfall.store.core.TestRun;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public interface StoreClientService {

  /**
   * Add a test run.
   * This should be called on the test machine before starting the client jobs.
   *
   * @param caseName          name of the test case to which the run is added.
   * @param className         fully qualified name of the test class.
   * @param terracottaVersion version of Terracotta.
   * @return run ID.
   */
  default long addRun(String caseName, String className, String terracottaVersion) {
    return 0L;
  }


  /**
   * Update the status of the current test run.
   * This should be called on the test machine after
   * the completion of the run. The original status is
   * INCOMPLETE. If the run completes successfully, then
   * update to COMPLETE. If the run fails with an exception,
   * update to FAILED.
   *
   * @param runId  current test run ID.
   * @param status updated status.
   */
  default boolean setStatus(long runId, TestRun.Status status) {
    return false;
  }

  /**
   * Add a client job with output artefacts to the current test run.
   * This should be called on the test machine after the client job is complete.
   *
   * @param runId        current test run ID.
   * @param clientNumber number of the client machine.
   * @param hostname     hostname of the client.
   * @param clientName   unique name of the client on the host.
   * @param details      Multiline description of the client job.
   * @param outputPath   Directory containing output files, e.g. HDR log files.
   * @return Client job ID.
   */
  default long addClientJob(long runId, int clientNumber, String hostname, String clientName, List<String> details, String outputPath) {
    return 0L;
  }


  /**
   * Add monitor log outputs.
   * This should be called on the machine where the monitor is running, after
   * the completion of all client jobs.
   *
   * @param runId    current test run ID.
   * @param host     host name of the machine where the monitor is running.
   * @param filename filename of the monitor output.
   * @param content  content of the monitor output.
   * @return Monitor log output ID.
   */
  default long addMetrics(long runId, String host, String filename, byte[] content) {
    return 0L;
  }

  /**
   * Compare the current run to the baseline, to detect possible
   * changes in performance of operations (regressions or progressions).
   * The result is based on a Kolmogorov-Smirnov test applied to
   * percentile distributions from the baseline and the current run,
   * respectively, for each operation. If no baseline is defined, the
   * result contains no changes.
   * <p>
   * This should be called on the test machine after the completion of the run.
   *
   * @param runId     current test run ID.
   * @param threshold threshold p-value.
   * @return {@ChangeReport} representing the operations for which
   * the test p-value is below the given threshold.
   */
  default ChangeReport checkRegression(long runId, double threshold) {
    return new ChangeReport(threshold);
  }
}
