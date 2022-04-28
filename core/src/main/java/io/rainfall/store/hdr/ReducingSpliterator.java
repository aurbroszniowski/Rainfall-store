package io.rainfall.store.hdr;

import java.util.Spliterator;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReducingSpliterator<T> implements Spliterator<T> {
  private final Spliterator<T> spliterator;
  private final BinaryOperator<T> accumulator;
  private final Supplier<T> identity;
  private final int ratio;

  public ReducingSpliterator(Spliterator<T> spliterator, Supplier<T> identity, BinaryOperator<T> accumulator, int ratio) {
    this.spliterator = spliterator;
    this.identity = identity;
    this.ratio = ratio;
    this.accumulator = accumulator;
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    @SuppressWarnings("unchecked")
    T[] reduction = (T[]) new Object[] { identity.get() };
    for (int i = 0; i < ratio; i++) {
      if (!spliterator.tryAdvance(t -> reduction[0] = accumulator.apply(reduction[0], t))) {
        return false;
      }
    }
    action.accept(reduction[0]);
    return true;
  }

  @Override
  public Spliterator<T> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    long uncompactedSize = spliterator.estimateSize();

    if (uncompactedSize == Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    } else {
      return uncompactedSize / ratio;
    }
  }

  @Override
  public int characteristics() {
    return 0;
  }
}
