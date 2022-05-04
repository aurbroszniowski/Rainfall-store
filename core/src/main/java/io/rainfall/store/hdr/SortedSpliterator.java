/*
 * Copyright (c) 2011-2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package io.rainfall.store.hdr;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterators.emptySpliterator;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("try")
public class SortedSpliterator<T> implements Spliterator<T> {

  /*
   * ORDERED:    if all shards are ordered (including the parent)
   * SORTED:     if all shards are sorted (including the parent)
   * DISTINCT:   if all shards are distinct (including the parent)
   *
   * SIZED:      if all shards are sized (parent is ambivalent)
   * NONNULL:    if all shards are non-null (parent is ambivalent)
   * IMMUTABLE:  if all shards are immutable (parent is ambivalent)
   * SUBSIZED:   if all shards are sub-sized (parent is ambivalent)
   *
   * CONCURRENT: if at least one shard is concurrent and the rest are either concurrent or immutable (parent is ambivalent)
   */
  private static final IntUnaryOperator CHARACTERISTIC_IDENTITY = a -> a | SIZED | NONNULL | IMMUTABLE | SUBSIZED;
  private static final IntBinaryOperator CHARACTERISTIC_MERGE = (a, b) -> (a & b) |
          //if one or more shards are concurrent and the rest are immutable then we're still concurrent
          ((((a | b) & (CONCURRENT | IMMUTABLE)) == (CONCURRENT | IMMUTABLE)) ? CONCURRENT : 0);

  private final List<Head<T>> heads;
  private final int characteristics;
  private final Comparator<Head<T>> headComparator;
  private final Comparator<? super T> valueComparator;

  public SortedSpliterator(Stream<? extends Supplier<? extends Spliterator<T>>> spliterators, Comparator<? super T> comparator, int characteristics) {
    List<Head<T>> suppliedHeads = spliterators
            .map((Function<Supplier<? extends Spliterator<T>>, ? extends Spliterator<T>>) Supplier::get)
            .map(Head::of).collect(toList());
    if (suppliedHeads.isEmpty()) {
      this.heads = singletonList(Head.of(emptySpliterator()));
    } else {
      this.heads = suppliedHeads;
    }
    this.valueComparator = requireNonNull(comparator);
    this.headComparator = (a, b) -> {
      if (a.empty() || b.empty()) {
        return a.empty() ? 1 : -1;
      } else {
        return valueComparator.compare(a.peek(), b.peek());
      }
    };
    this.characteristics = heads.stream().mapToInt(h -> h.spliterator().characteristics())
            .reduce(CHARACTERISTIC_IDENTITY.applyAsInt(characteristics) , CHARACTERISTIC_MERGE);

    //Remote spliterators do not report sorted correctly so we cannot make this assertion
    //if ((this.characteristics & (SORTED | ORDERED)) != (SORTED | ORDERED)) {
    //  throw new IllegalArgumentException("Sequenced spliterators must be sorted and ordered");
    //}
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    Head<T> minHead = Collections.min(heads, headComparator);
    if (minHead.empty()) {
      return false;
    } else {
      action.accept(minHead.retrieve());
      return true;
    }
  }

  @Override
  public Spliterator<T> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    try {
      return heads.stream().mapToLong(h -> h.spliterator().estimateSize()).reduce(Math::addExact).orElse(0L);
    } catch (ArithmeticException e) {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public int characteristics() {
    return characteristics;
  }

  @Override
  public Comparator<? super T> getComparator() {
    return valueComparator;
  }

  private static class Head<T> {

    private final Object EMPTY_SENTINEL = new Object();
    private final Object CLEAN_SENTINEL = new Object();

    public static <T> Head<T> of(Spliterator<T> spliterator) {
      return new Head<>(spliterator);
    }

    private final Spliterator<T> spliterator;

    private Object value = CLEAN_SENTINEL;

    private Head(Spliterator<T> spliterator) {
      this.spliterator = spliterator;
    }

    @SuppressWarnings("unchecked")
    private T retrieve() {
      Object val = peekInternal();
      if (val == EMPTY_SENTINEL) {
        throw new IllegalStateException();
      } else {
        advance();
        return (T) val;
      }
    }

    private void advance() {
      if (!spliterator.tryAdvance(s -> value = s)) {
        value = EMPTY_SENTINEL;
      }
    }

    public boolean empty() {
      return peekInternal() == EMPTY_SENTINEL;
    }

    private Object peekInternal() {
      if (value == CLEAN_SENTINEL) {
        advance();
      }
      return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
      Object val = peekInternal();
      if (val == EMPTY_SENTINEL) {
        throw new IllegalStateException();
      } else {
        return (T) val;
      }
    }

    public Spliterator<T> spliterator() {
      return spliterator;
    }
  }
}
