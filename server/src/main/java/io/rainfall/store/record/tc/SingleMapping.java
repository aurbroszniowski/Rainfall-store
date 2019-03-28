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

import com.terracottatech.store.Cell;
import com.terracottatech.store.Record;
import com.terracottatech.store.definition.CellDefinition;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singletonList;

class SingleMapping<K extends Comparable<K>, T, B, V> implements Mapping<K, T, B> {

  static <K extends Comparable<K>, T, B, V> SingleMapping<K, T, B, V> of(CellDefinition<V> cellDefinition,
                                                                         Function<T, V> getter,
                                                                         BiFunction<B, V, B> setter) {
    return new SingleMapping<>(cellDefinition, getter, setter, null);
  }

  static <K extends Comparable<K>, T, B, V> SingleMapping<K, T, B, V> of(CellDefinition<V> cellDefinition,
                                                                         Function<T, V> getter,
                                                                         BiFunction<B, V, B> setter,
                                                                         V defaultValue) {
    return new SingleMapping<>(cellDefinition, getter, setter, defaultValue);
  }

  private final CellDefinition<V> cellDefinition;
  private final Function<T, V> getter;
  private final BiFunction<B, V, B> setter;
  private final V defaultValue;

  private SingleMapping(CellDefinition<V> cellDefinition,
                        Function<T, V> getter,
                        BiFunction<B, V, B> setter,
                        V defaultValue) {
    this.cellDefinition = cellDefinition;
    this.getter = getter;
    this.setter = setter;
    this.defaultValue = defaultValue;
  }

  @Override
  public List<Cell<?>> newCell(T object) {
    V value = getter.apply(object);
    Cell<V> cell = cellDefinition.newCell(value);
    return singletonList(cell);
  }

  @Override
  public void setValue(Record<K> cells, B builder) {
    V value = cells.get(cellDefinition).orElse(defaultValue);
    setter.apply(builder, value);
  }
}
