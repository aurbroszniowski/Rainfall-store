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

import com.terracottatech.store.Cell;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetReader;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.CellDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

abstract class ChildDataset<P extends Comparable<P>, V, B extends Builder<V>, R extends Rec<Long, V>>
    extends TcDataset<Long, V, B, R> {

  private static final Comparator<Record<Long>> ID_COMPARATOR = Record.<Long>keyFunction()
      .asComparator();

  private final TcDataset<P, ?, ?, ?> parent;
  private final CellDefinition<P> parentKey;
  private final AtomicLong idGenerator;


  ChildDataset(TcDataset<P, ?, ?, ?> parent,
               CellDefinition<P> parentKey,
               Dataset<Long> dataset,
               List<? extends Mapping<Long, V, B>> mappings) {
    this(parent, parentKey, dataset, mappings, mappings);
  }

  ChildDataset(TcDataset<P, ?, ?, ?> parent,
               CellDefinition<P> parentKey,
               Dataset<Long> dataset,
               List<? extends Mapping<Long, V, B>> mappings,
               List<? extends Mapping<Long, V, B>> listedMappings) {
    super(dataset, mappings, listedMappings);
    this.parent = parent;
    this.parentKey = parentKey;
    this.idGenerator = initIdGenerator();
  }

  void indexParent() {
    createIndex(parentKey);
  }

  private AtomicLong initIdGenerator() {
    Long lastId = lastId();
    return new AtomicLong(lastId);
  }

  private Long lastId() {
    DatasetReader<Long> reader = reader();
    return reader.records()
        .max(ID_COMPARATOR)
        .map(Record::getKey)
        .orElse(0L);
  }

  long add(P parentId, V object) {
    if (parent.contains(parentId)) {
      Cell<P> parentCell = parentKey.newCell(parentId);
      Stream<Cell<?>> cells = concat(of(parentCell), toCells(object));
      return addCells(cells);
    } else {
      String msg = format("Parent ID %s not found while adding %s.", parentId, object);
      throw new IllegalStateException(msg);
    }
  }

  private Long addCells(Stream<Cell<?>> cells) {
    Long key = key();
    addCells(key, cells);
    return key;
  }

  private Long key() {
    return idGenerator.incrementAndGet();
  }

  List<R> list(P parentId) {
    return children(parentId)
        .map(this::fromListedRecord)
        .collect(toList());
  }

  Stream<Record<Long>> children(P parentId) {
    Predicate<Record<?>> exists = parentKey
        .value()
        .is(parentId);
    return filter(exists);
  }

  @Override
  R fromRecord(Record<Long> record, V value) {
    P parentID = parentID(record);
    long key = record.getKey();
    Long timeStamp = timeStamp(record);
    return record(parentID, key, value, timeStamp);
  }

  private P parentID(Record<Long> record) {
    return record.get(parentKey)
        .orElseThrow(() -> new IllegalStateException("Missing time parent key."));
  }

  protected abstract R record(P parentKey, Long key, V value, Long timeStamp);
}
