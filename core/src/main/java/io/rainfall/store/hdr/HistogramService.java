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

package io.rainfall.store.hdr;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.HistogramLogReader;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.unmodifiableSet;
import static java.util.Spliterator.ORDERED;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

public class HistogramService {

  private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;
  private static final int DEFAULT_MAX_DATA_POINTS = 200;
  private static final int N_THREADS = 1;
  private static final int NUM_FIXED_PERCENTILE_POINTS = 10;

  private final Executor executor = Executors.newFixedThreadPool(N_THREADS);

  private final KolmogorovSmirnovTest statisticsTest = new KolmogorovSmirnovTest();

  public HdrData readHdrData(Supplier<InputStream> supplier) {
    return readHdrData(supplier, DEFAULT_MAX_DATA_POINTS);
  }

  HdrData readHdrData(Supplier<InputStream> supplier, int maxDataPoints) {
    checkMaxDataPoints(maxDataPoints);
    return supplyAndGet(() -> toHdrData(compactTo(readLog(supplier), maxDataPoints)));
  }

  private HdrData supplyAndGet(Supplier<HdrData> hdrSupplier) {
    try {
      return supplyAsync(hdrSupplier, executor).get();
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private Stream<Histogram> readLog(Supplier<InputStream> supplier) {
    return histograms(supplier.get());
  }

  private Stream<Histogram> histograms(InputStream is) {
    HistogramLogReader logReader = new HistogramLogReader(is);
    Iterator<Histogram> iterator = histogramIterator(logReader);
    Spliterator<Histogram> spliterator = Spliterators.spliteratorUnknownSize(iterator, ORDERED);
    return stream(spliterator, false).onClose(() -> {
      try {
        is.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private Iterator<Histogram> histogramIterator(HistogramLogReader logReader) {
    return new Iterator<Histogram>() {
      @Override
      public boolean hasNext() {
        return logReader.hasNext();
      }

      @Override
      public Histogram next() {
        return (Histogram)logReader.nextIntervalHistogram();
      }
    };
  }

  private HdrData toHdrData(Stream<Histogram> reducedList) {
    Histogram total = new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    HdrData.Builder builder = HdrData.builder();
    reducedList.forEachOrdered(histogram -> {
      addDatapoint(builder, histogram);
      total.add(histogram);
    });
    builder.roundedPercentiles(percentiles(total));
    builder.fixedPercentileValues(fixedPercentileValues(total));
    for (HistogramIterationValue value : total.percentiles(5)) {
      double point = value.getPercentileLevelIteratedTo() / 100.0D;
      builder.addPercentile(point, value.getValueIteratedTo());
    }
    return builder.build();
  }

  private List<Long> fixedPercentileValues(Histogram total) {
    return fixedPercentilePoints(NUM_FIXED_PERCENTILE_POINTS)
        .map(n -> n * 100)
        .map(total::getValueAtPercentile)
        .collect(toList());
  }

  static Stream<Double> fixedPercentilePoints(int num) {
    return Stream.iterate(0.0, n -> n + ((1.0 - n) / 2.0))
        .limit(num);
  }

  private Map<Percentile, Long> percentiles(Histogram acc) {
    return Percentile.all()
        .collect(toMap(identity(), p -> acc.getValueAtPercentile(p.getValue())));
  }

  private void addDatapoint(HdrData.Builder builder, Histogram histogram) {
    long startTimeStamp = histogram.getStartTimeStamp();
    long durationInMs = histogram.getEndTimeStamp() - startTimeStamp;
    double tps = 1000.0 * histogram.getTotalCount() / durationInMs;
    double mean = histogram.getMean() / 1000000;
    double error = histogram.getStdDeviation() / 1000000;
    builder.addStartTime(startTimeStamp)
        .addTps(tps)
        .addMean(mean)
        .addError(error);
    Percentile.all()
        .forEach(p -> builder.addTimedPercentile(p, histogram.getValueAtPercentile(p.getValue())));
  }

  public HdrData aggregateHdrData(List<Supplier<InputStream>> inputStreams) {
    return aggregateHdrData(inputStreams, DEFAULT_MAX_DATA_POINTS);
  }

  HdrData aggregateHdrData(List<Supplier<InputStream>> inputStreams, int maxDataPoints) {
    checkMaxDataPoints(maxDataPoints);
    return inputStreams.isEmpty() ? blankHdrData() : readAndAggregate(inputStreams, maxDataPoints);
  }

  private void checkMaxDataPoints(int maxDataPoints) {
    if (maxDataPoints <= 0) {
      throw new IllegalArgumentException("maxDataPoints must be positive, was " + maxDataPoints);
    }
  }

  private HdrData blankHdrData() {
    Histogram blank = new Histogram(0);
    return HdrData.builder()
        .roundedPercentiles(percentiles(blank))
        .fixedPercentileValues(fixedPercentileValues(blank))
        .build();
  }

  private HdrData readAndAggregate(List<Supplier<InputStream>> suppliers, int maxDataPoints) {
    return supplyAndGet(() -> {
      List<Stream<Histogram>> components = suppliers.stream().map(this::readLog).collect(toList());
      return toHdrData(compactTo(aggregate(components), maxDataPoints));
    });
  }

  private static Stream<Histogram> aggregate(List<Stream<Histogram>> inputs) {
    Stream.Builder<Supplier<Spliterator<Histogram>>> builder = Stream.builder();
    for (int i = 0; i < inputs.size(); i++) {
      final String tag = Integer.toString(i);
      builder.add(inputs.get(i).peek(histogram -> histogram.setTag(tag))::spliterator);
    }
    SortedSpliterator<Histogram> sortedComponents = new SortedSpliterator<>(builder.build(), Comparator.comparing(Histogram::getStartTimeStamp), 0);

    Set<String> completeSet = unmodifiableSet(IntStream.range(0, inputs.size()).mapToObj(Integer::toString).collect(toSet()));

    Spliterator<Histogram> aggregator = new Spliterator<Histogram>() {

      private final Map<String, Histogram> currentAssembly = new HashMap<>();
      @Override
      public boolean tryAdvance(Consumer<? super Histogram> action) {
        AtomicBoolean collected = new AtomicBoolean();
        do {
          if (!sortedComponents.tryAdvance(histogram -> {
            Histogram existing = currentAssembly.putIfAbsent(histogram.getTag(), histogram);
            if (existing == null) {
              if (currentAssembly.keySet().equals(completeSet)) {
                Histogram h = new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
                currentAssembly.values().forEach(h::add);
                currentAssembly.clear();
                collected.set(true);
                action.accept(h);
              }
            } else {
              currentAssembly.clear();
              currentAssembly.putIfAbsent(histogram.getTag(), histogram);
            }
          })) {
            return false;
          }
        } while (!collected.get());

        return true;
      }

      @Override
      public Spliterator<Histogram> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        long unaggregatedSize = sortedComponents.estimateSize();
        if (unaggregatedSize == Long.MAX_VALUE) {
          return Long.MAX_VALUE;
        } else {
          return unaggregatedSize / completeSet.size();
        }
      }

      @Override
      public int characteristics() {
        return 0;
      }
    };

    return StreamSupport.stream(aggregator, false);
  }

  private static Stream<Histogram> compactTo(Stream<Histogram> histograms, int maxDataPoints) {
    try {
      List<Histogram> collected = histograms.collect(toList());
      if (collected.size() <= maxDataPoints) {
        return collected.stream();
      } else {
        int ratio = (int) Math.ceil(((double) collected.size()) / maxDataPoints);
        return StreamSupport.stream(new ReducingSpliterator<>(collected.stream().spliterator(), () -> new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS), (a, b) -> {
          a.add(b);
          return a;
        }, ratio), false);
      }
    } finally {
      histograms.close();
    }
  }

  public Double comparePercentiles(HdrData x, HdrData y) {
    double[] xvals = x.getFixedPercentileValues();
    double[] yvals = y.getFixedPercentileValues();
    return statisticsTest.kolmogorovSmirnovTest(xvals, yvals);
  }
}
