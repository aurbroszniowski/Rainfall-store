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
import io.rainfall.store.record.ClientJobRec;

import com.terracottatech.store.Dataset;
import com.terracottatech.store.definition.LongCellDefinition;

import java.util.List;

import static com.terracottatech.store.definition.CellDefinition.defineInt;
import static com.terracottatech.store.definition.CellDefinition.defineLong;
import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.record.tc.SingleMapping.of;
import static java.util.Arrays.asList;

class JobDataset extends ChildDataset<Long, ClientJob, ClientJob.Builder, ClientJobRec> {

  private static final LongCellDefinition RUN_ID = defineLong("runId");

  private static final List<SingleMapping<Long, ClientJob, ClientJob.Builder, ?>> MAPPINGS = asList(
      of(
          defineString("host"),
          ClientJob::getHost,
          ClientJob.Builder::host
      ),
      of(
          defineString("symbolicName"),
          ClientJob::getSymbolicName,
          ClientJob.Builder::symbolicName
      ),
      of(
          defineInt("clientNumber"),
          ClientJob::getClientNumber,
          ClientJob.Builder::clientNumber
      ),
      of(
          defineString("details"),
          ClientJob::getDetails,
          ClientJob.Builder::details
      )
  );

  JobDataset(TcDataset<Long, ?, ?, ?> parent, Dataset<Long> dataset) {
    super(parent, RUN_ID, dataset, MAPPINGS);
  }

  @Override
  protected ClientJobRec record(Long parentKey, Long key, ClientJob value, Long timeStamp) {
    return new ClientJobRec(parentKey, key, value, timeStamp);
  }

  @Override
  ClientJob.Builder builder() {
    return ClientJob.builder();
  }
}
