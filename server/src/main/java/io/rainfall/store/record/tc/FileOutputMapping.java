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

import io.rainfall.store.core.FileOutput;
import io.rainfall.store.data.CompressionFormat;
import io.rainfall.store.data.Payload;

import com.terracottatech.store.Cell;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.BytesCellDefinition;
import com.terracottatech.store.definition.IntCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;

import java.util.List;

import static com.terracottatech.store.definition.CellDefinition.defineBytes;
import static com.terracottatech.store.definition.CellDefinition.defineInt;
import static com.terracottatech.store.definition.CellDefinition.defineString;
import static io.rainfall.store.data.CompressionFormat.ZIP;
import static io.rainfall.store.data.Payload.of;
import static java.util.Arrays.asList;

class FileOutputMapping<F extends FileOutput, B extends FileOutput.Builder> implements Mapping<Long, F, B> {

  private static final BytesCellDefinition DATA = defineBytes("data");
  private static final StringCellDefinition COMPRESSION_FORMAT = defineString("compressionFormat");
  private static final IntCellDefinition ORIGINAL_LENGTH = defineInt("originalLength");

  private static final CompressionFormat DEFAULT_FORMAT = ZIP;
  private static final int DEFAUL_LENGTH = -1;

  @Override
  public List<Cell<?>> newCell(F fileOutput) {
    Payload payload = fileOutput.getPayload();
    return asList(
        DATA.newCell(payload.getData()),
        COMPRESSION_FORMAT.newCell(payload.getFormat().name()),
        ORIGINAL_LENGTH.newCell(payload.getOriginalLength())
    );
  }

  @Override
  public void setValue(Record<Long> cells, B builder) {
    byte[] data = cells.get(DATA)
        .orElse(null);
    CompressionFormat format = cells.get(COMPRESSION_FORMAT)
        .map(CompressionFormat::valueOf)
        .orElse(DEFAULT_FORMAT);
    int originalLength = cells.get(ORIGINAL_LENGTH)
        .orElse(DEFAUL_LENGTH);
    Payload payload = of(data, format, originalLength);
    builder.payload(payload);
  }
}
