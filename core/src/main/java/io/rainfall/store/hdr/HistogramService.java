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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Spliterator.ORDERED;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class HistogramService {

  private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;
  private static final int DEFAULT_MAX_DATA_POINTS = 200;
  private static final int N_THREADS = 1;
  private static final int NUM_FIXED_PERCENTILE_POINTS = 10;

  private static final Collector<Histogram, ?, Histogram> SUMMING = reducing(
      new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS),
      HistogramService::sum
  );

  private final Executor executor = Executors.newFixedThreadPool(N_THREADS);

  private final KolmogorovSmirnovTest statisticsTest = new KolmogorovSmirnovTest();

  public HdrData readHdrData(Supplier<InputStream> supplier) {
    return readHdrData(supplier, DEFAULT_MAX_DATA_POINTS);
  }

  HdrData readHdrData(Supplier<InputStream> supplier, int maxDataPoints) {
    checkMaxDataPoints(maxDataPoints);
    return supplyAndGet(
        () -> readTimeRange(supplier)
            .map(range -> readHdrDataInRange(supplier, range, maxDataPoints))
            .orElseGet(this::blankHdrData)
    );
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

  private HdrData readHdrDataInRange(Supplier<InputStream> supplier, TimeRange range, int maxDataPoints) {
    List<Histogram> histograms = readLog(supplier, range, maxDataPoints);
    return toHdrData(histograms);
  }

  private List<Histogram> readLog(Supplier<InputStream> supplier, TimeRange range, int maxDataPoints) {
    try (InputStream is = supplier.get()) {
      return readLog(is, range, maxDataPoints);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Histogram> readLog(InputStream is, TimeRange frame, int maxDataPoints) {
    double interval = getInterval(frame, maxDataPoints);
    return histograms(is)
        .map(TimeEntry::new)
        .filter(frame::contains)
        .collect(bucketingThenSumming(frame, interval))
        .entrySet()
        .stream()
        .limit(maxDataPoints)
        .map(Map.Entry::getValue)
        .collect(toList());
  }

  private double getInterval(TimeRange range, int maxDataPoints) {
    double length = range.length();
    if (length == 0.0) {
      throw new IllegalArgumentException("Cannot parse hlog with empty time range: " + range);
    }
    return length / maxDataPoints;
  }

  private Stream<Histogram> histograms(InputStream is) {
    HistogramLogReader logReader = new HistogramLogReader(is);
    Iterator<Histogram> iterator = histogramIterator(logReader);
    Spliterator<Histogram> spliterator = Spliterators.spliteratorUnknownSize(iterator, ORDERED);
    return stream(spliterator, false);
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

  private Collector<TimeEntry, ?, Map<Long, Histogram>> bucketingThenSumming(
      TimeRange frame, double interval) {
    return groupingBy(
        timeEntry -> bucketNumber(timeEntry, frame.start, interval),
        TreeMap::new,
        mapping(TimeEntry::getHistogram, SUMMING)
    );
  }

  private long bucketNumber(TimeEntry timeEntry, long start, double interval) {
    double div = (timeEntry.time - start) / interval;
    return (long)div;
  }

  private HdrData toHdrData(Collection<Histogram> reducedList) {
    Histogram total = new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    HdrData.Builder builder = HdrData.builder();
    for (Histogram histogram : reducedList) {
      addDatapoint(builder, histogram);
      total.add(histogram);
    }
    builder.roundedPercentiles(percentiles(total));
    builder.fixedPercentileValues(fixedPercentileValues(total));
    for (HistogramIterationValue value : total.percentiles(5)) {
      double point = value.getPercentileLevelIteratedTo() / 100.0D;
      builder.addPercentile(point, value.getValueIteratedTo());
    }
    return builder.build();
  }

  private List<Long> fixedPercentileValues(Histogram total) {
    System.out.println();
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
    return inputStreams.isEmpty()
        ? blankHdrData()
        : readAndAggregate(inputStreams, maxDataPoints);
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
      TimeRange frame = timeFrameIntersection(suppliers);
      List<Histogram> aggregated = suppliers.stream()
          .map(supplier -> readLog(supplier, frame, maxDataPoints))
          .reduce(this::sum)
          .orElseThrow(IllegalStateException::new);
      return toHdrData(aggregated);
    });
  }

  private TimeRange timeFrameIntersection(List<Supplier<InputStream>> suppliers) {
    List<TimeRange> timeRanges = suppliers.stream()
        .map(this::readTimeRange)
        .map(o -> o.orElseThrow(() -> new IllegalArgumentException("Cannot aggregate a blank log.")))
        .collect(toList());
    long latestStart = timeRanges.stream()
        .mapToLong(tr -> tr.start)
        .max()
        .orElseThrow(IllegalStateException::new);
    long earliestEnd = timeRanges.stream()
        .mapToLong(tr -> tr.end)
        .min()
        .orElseThrow(IllegalStateException::new);
    return new TimeRange(latestStart, earliestEnd);
  }

  private Optional<TimeRange> readTimeRange(Supplier<InputStream> s) {
    try (InputStream is = s.get()) {
      HistogramLogReader logReader = new HistogramLogReader(is);
      if (logReader.hasNext()) {
        long start = logReader.nextIntervalHistogram()
            .getStartTimeStamp();
        long end = start;
        while (logReader.hasNext()) {
          end = logReader.nextIntervalHistogram()
              .getStartTimeStamp();
        }
        TimeRange timeRange = new TimeRange(start, end);
        return Optional.of(timeRange);
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Histogram> sum(List<Histogram> timeEntries1, List<Histogram> timeEntries2) {
    int size = Math.min(timeEntries1.size(), timeEntries2.size());
    Iterator<Histogram> it1 = timeEntries1.iterator();
    Iterator<Histogram> it2 = timeEntries2.iterator();
    return Stream.generate(() -> sum(it1.next(), it2.next()))
        .limit(size)
        .collect(toList());
  }

  private static Histogram sum(Histogram h1, Histogram h2) {
    Histogram sum = new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    sum.add(h1);
    sum.add(h2);
    return sum;
  }

  static long roundTime(long timeMillis) {
    return (long)(Math.floor(timeMillis / 1000) * 1000);
  }

  public Double comparePercentiles(HdrData x, HdrData y) {
    double[] xvals = x.getFixedPercentileValues();
    double[] yvals = y.getFixedPercentileValues();
    return statisticsTest.kolmogorovSmirnovTest(xvals, yvals);
  }

  private static class TimeRange {
    private final long start;
    private final long end;

    private TimeRange(long start, long end) {
      this.start = roundTime(start);
      this.end = roundTime(end);
    }

    private long length() {
      return end - start;
    }

    private boolean contains(TimeEntry timeEntry) {
      return timeEntry.time >= start && timeEntry.time <= end;
    }

    @Override
    public String toString() {
      return "(" + start + ", " + end + ")";
    }
  }

  private static class TimeEntry {
    private final long time;
    private final Histogram histogram;

    private TimeEntry(Histogram histogram) {
      this.time = roundTime(histogram.getStartTimeStamp());
      this.histogram = histogram;
    }

    private Histogram getHistogram() {
      return histogram;
    }
  }
}
