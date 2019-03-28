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

import io.rainfall.store.core.StatsLog;
import io.rainfall.store.record.StatsRec;

import com.terracottatech.store.Dataset;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;

import java.util.List;

import static com.terracottatech.store.definition.CellDefinition.defineLong;
import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.record.tc.SingleMapping.of;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class StatsDataset extends ChildDataset<Long, StatsLog, StatsLog.Builder, StatsRec> {

  private static final LongCellDefinition RUN_ID = defineLong("runId");
  private static final StringCellDefinition HOST = defineString("host");

  private static final List<? extends Mapping<Long, StatsLog, StatsLog.Builder>> MAPPINGS = asList(
      new FileOutputMapping<>(),
      of(
          HOST,
          StatsLog::getHost,
          StatsLog.Builder::host
      ),
      of(
          defineString("type"),
          StatsLog::getType,
          StatsLog.Builder::type
      )
  );

  StatsDataset(RunDataset parent, Dataset<Long> dataset) {
    super(parent, RUN_ID, dataset, MAPPINGS);
  }

  @Override
  protected StatsRec record(Long parentKey, Long key, StatsLog value, Long timeStamp) {
    return new StatsRec(parentKey, key, value, timeStamp);
  }

  @Override
  StatsLog.Builder builder() {
    return StatsLog.builder();
  }

  public List<StatsRec> list(long runId, String host) {
    return children(runId)
        .filter(HOST.value().is(host))
        .map(this::fromListedRecord)
        .collect(toList());
  }
}
