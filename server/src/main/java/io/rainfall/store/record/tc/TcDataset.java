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


import io.rainfall.store.core.Builder;
import io.rainfall.store.record.Rec;
import org.slf4j.LoggerFactory;

import com.terracottatech.store.Cell;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetReader;
import com.terracottatech.store.Record;
import com.terracottatech.store.UpdateOperation;
import com.terracottatech.store.definition.CellDefinition;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.indexing.Index;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.terracottatech.store.UpdateOperation.write;
import static com.terracottatech.store.definition.CellDefinition.defineLong;
import static com.terracottatech.store.indexing.IndexSettings.BTREE;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

abstract class TcDataset<K extends Comparable<K>, V, B extends Builder<V>, R extends Rec<K, V>> {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TcDataset.class);

  private static final LongCellDefinition TIME_STAMP = defineLong("timeStamp");

  private final Dataset<K> dataset;
  private final List<? extends Mapping<K, V, B>> mappings;
  private final List<? extends Mapping<K, V, B>> listedMappings;

  TcDataset(Dataset<K> dataset, List<? extends Mapping<K, V, B>> mappings) {
    this(dataset, mappings, mappings);
  }

  TcDataset(Dataset<K> dataset, List<? extends Mapping<K, V, B>> mappings,
            List<? extends Mapping<K, V, B>> listedMappings) {
    this.dataset = dataset;
    this.mappings = mappings;
    this.listedMappings = listedMappings;
  }

  DatasetReader<K> reader() {
    return dataset.reader();
  }

  void addCells(K key, Stream<Cell<?>> cells) {
    dataset.writerReader()
        .add(key, cells.collect(toList()));
  }

  Stream<Cell<?>> toCells(V value) {
    return concat(
        mappedCells(value),
        of(timeStampCell())
    );
  }

  private Stream<Cell<?>> mappedCells(V value) {
    return mappings.stream()
        .map(mapping -> mapping.newCell(value))
        .flatMap(List::stream);
  }

  private Cell<Long> timeStampCell() {
    long time = now().getEpochSecond();
    return TIME_STAMP.newCell(time);
  }

  Optional<R> get(K id) {
    return tcRecord(id).map(this::fromRecord);
  }

  private Optional<Record<K>> tcRecord(K id) {
    return dataset.reader().get(id);
  }

  List<R> list() {
    return dataset.reader()
        .records()
        .map(this::fromRecord)
        .collect(toList());
  }

  R fromRecord(Record<K> record) {
    return fromRecord(record, mappings);
  }

  private R fromRecord(Record<K> record, List<? extends Mapping<K, V, B>> mappings) {
    B builder = builder();
    mappings.forEach(mapping -> mapping.setValue(record, builder));
    V value = builder.build();
    return fromRecord(record, value);
  }

  abstract R fromRecord(Record<K> record, V value);

  Long timeStamp(Record<K> record) {
    return record.get(TIME_STAMP)
        .orElseThrow(() -> new IllegalStateException("Missing time stamp."));
  }

  R fromListedRecord(Record<K> record) {
    List<? extends Mapping<K, V, B>> listedMappings = mappings.stream()
        .filter(this.listedMappings::contains)
        .collect(toList());
    return fromRecord(record, listedMappings);
  }

  abstract B builder();

  boolean contains(K id) {
    return tcRecord(id).isPresent();
  }

  Stream<Record<K>> filter(Predicate<Record<?>> predicate) {
    return dataset.reader()
        .records()
        .explain(plan -> LOGGER.debug("Filtering on {}:\n{}.", predicate, plan))
        .filter(predicate);
  }

  <C> boolean update(K ID, CellDefinition<C> def, C value) {
    UpdateOperation<K> op = write(def).value(value);
    return dataset.writerReader().update(ID, op);
  }

  <P extends Comparable<P>> void createIndex(CellDefinition<P> parentKey) {
    if (containsIndexOn(parentKey)) {
      LOGGER.info("Index already exists on {}.", parentKey);
    } else {
      dataset.getIndexing()
          .createIndex(parentKey, BTREE)
          .whenComplete(this::processIndexingOperation);
    }
  }

  private <P extends Comparable<P>> void processIndexingOperation(Index<P> idx, Throwable e) {
    if (idx != null) {
      LOGGER.info("Index created on {}.", idx.on());
    }
    if (e != null) {
      LOGGER.error("Failed to create index: {}.", e.getMessage());
      throw new IllegalStateException(e);
    }
  }

  private <P extends Comparable<P>> boolean containsIndexOn(CellDefinition<P> parentKey) {
    return dataset.getIndexing()
        .getAllIndexes()
        .stream()
        .anyMatch(idx -> cellDefinitionsEqual(parentKey, idx.on()));
  }

  private boolean cellDefinitionsEqual(CellDefinition<?> def1, CellDefinition<?> def2) {
    return Objects.equals(def1.name(), def2.name())
           && Objects.equals(def1.type(), def2.type());
  }

  void close() {
    this.dataset.close();
  }
}
