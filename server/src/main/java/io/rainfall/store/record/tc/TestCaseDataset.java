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

import io.rainfall.store.core.TestCase;
import io.rainfall.store.record.DuplicateNameException;
import io.rainfall.store.record.TestCaseRec;

import com.terracottatech.store.Cell;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.Record;
import com.terracottatech.store.function.BuildableComparableFunction;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.record.tc.SingleMapping.of;

class TestCaseDataset extends TcDataset<String, TestCase, TestCase.Builder, TestCaseRec> {

  private static final BuildableComparableFunction<Record<String>, String> KEY_FUNCTION = Record.keyFunction();

  private static final List<SingleMapping<String, TestCase, TestCase.Builder, ?>> MAPPINGS = Collections.singletonList(
      of(
          defineString("description"),
          TestCase::getDescription,
          TestCase.Builder::description
      )
  );

  TestCaseDataset(Dataset<String> dataset) {
    super(dataset, MAPPINGS);
  }

  void add(String uniqueName, TestCase testCase) {
    if (keyExists(uniqueName)) {
      throw new DuplicateNameException(uniqueName);
    }
    Stream<Cell<?>> cells = toCells(testCase);
    super.addCells(uniqueName, cells);
  }

  private boolean keyExists(String uniqueName) {
    return reader().records()
        .anyMatch(KEY_FUNCTION.is(uniqueName));
  }

  @Override
  TestCaseRec fromRecord(Record<String> record, TestCase value) {
    String key = record.getKey();
    Long timeStamp = timeStamp(record);
    return new TestCaseRec(key, value, timeStamp);
  }

  @Override
  TestCase.Builder builder() {
    return TestCase.builder();
  }
}
