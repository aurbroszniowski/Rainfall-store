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

import io.rainfall.store.core.TestRun;
import io.rainfall.store.record.RunRec;

import com.terracottatech.store.Dataset;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.BoolCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;

import java.util.List;
import java.util.Optional;

import static com.terracottatech.store.definition.CellDefinition.defineBool;
import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.core.TestRun.Status.UNKNOWN;
import static io.rainfall.store.record.tc.SingleMapping.of;
import static java.util.Arrays.asList;

class RunDataset extends ChildDataset<String, TestRun, TestRun.Builder, RunRec> {

  private static final StringCellDefinition TEST_CASE_NAME = defineString("caseName");

  private static final StringCellDefinition STATUS = defineString("status");

  private static final BoolCellDefinition BASELINE = defineBool("baseline");

  private static final List<SingleMapping<Long, TestRun, TestRun.Builder, ?>> MAPPINGS = asList(
      of(
          defineString("version"),
          TestRun::getVersion,
          TestRun.Builder::version
      ),
      of(
          defineString("className"),
          TestRun::getClassName,
          TestRun.Builder::className
      ),
      of(
          defineString("checksum"),
          TestRun::getChecksum,
          TestRun.Builder::checksum
      ),
      of(
          STATUS,
          run -> run.getStatus().name(),
          TestRun.Builder::status,
          UNKNOWN.name()
      ),
      of(
          BASELINE,
          TestRun::isBaseline,
          TestRun.Builder::baseline,
          false
      )
  );

  RunDataset(TcDataset<String, ?, ?, ?> testCases, Dataset<Long> dataset) {
    super(testCases, TEST_CASE_NAME, dataset, MAPPINGS);
  }

  @Override
  protected RunRec record(String parentKey, Long key, TestRun value, Long timeStamp) {
    return new RunRec(parentKey, key, value, timeStamp);
  }

  @Override
  TestRun.Builder builder() {
    return TestRun.builder();
  }

  boolean setStatus(long ID, TestRun.Status status) {
    return update(ID, STATUS, status.name());
  }

  boolean setBaseline(long ID, boolean value) {
    return update(ID, BASELINE, value);
  }

  Optional<Long> getLastBaselineID() {
    return filter(r -> r.get(BASELINE).orElse(false))
        .map(Record::getKey)
        .reduce((a, b) -> b);
  }
}
