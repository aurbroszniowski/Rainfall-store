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

import io.rainfall.store.core.OperationOutput;
import io.rainfall.store.record.OutputRec;

import com.terracottatech.store.Dataset;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.terracottatech.store.definition.CellDefinition.defineLong;
import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.record.tc.SingleMapping.of;
import static java.util.Arrays.asList;

class OutputDataset extends ChildDataset<Long, OperationOutput, OperationOutput.Builder, OutputRec> {

  private static final LongCellDefinition JOB_ID = defineLong("jobId");

  private static final SingleMapping<Long, OperationOutput, OperationOutput.Builder, String> FORMAT_MAPPING = of(
      defineString("format"),
      OperationOutput::getFormat,
      OperationOutput.Builder::format
  );

  private static final StringCellDefinition OPERATION = defineString("operation");
  private static final SingleMapping<Long, OperationOutput, OperationOutput.Builder, String> OPERATION_MAPPING = of(
      OPERATION,
      OperationOutput::getOperation,
      OperationOutput.Builder::operation
  );

  private static final List<? extends Mapping<Long, OperationOutput, OperationOutput.Builder>> MAPPINGS = asList(
      new FileOutputMapping<>(),
      FORMAT_MAPPING,
      OPERATION_MAPPING
  );

  private static final List<? extends Mapping<Long, OperationOutput, OperationOutput.Builder>> LISTED_MAPPINGS = asList(
      FORMAT_MAPPING,
      OPERATION_MAPPING
  );

  OutputDataset(TcDataset<Long, ?, ?, ?> runs, Dataset<Long> dataset) {
    super(runs, JOB_ID, dataset, MAPPINGS, LISTED_MAPPINGS);
  }

  @Override
  protected OutputRec record(Long parentKey, Long key, OperationOutput value, Long timeStamp) {
    return new OutputRec(parentKey, key, value, timeStamp);
  }

  Stream<String> getOperations(long pareintID) {
    return children(pareintID)
        .map(r -> r.get(OPERATION).orElseThrow(
            () -> new IllegalStateException("Operation missing in output " + r.getKey())));
  }

  Stream<OutputRec> getOutputsForOperation(long pareintID, String operation) {
    Predicate<Record<?>> predicate = OPERATION.value().is(operation);
    return children(pareintID)
        .filter(predicate)
        .map(this::fromRecord);
  }

  @Override
  OperationOutput.Builder builder() {
    return OperationOutput.builder();
  }
}
