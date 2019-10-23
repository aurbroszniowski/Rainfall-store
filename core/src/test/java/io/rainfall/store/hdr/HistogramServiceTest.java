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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.rainfall.store.hdr.HistogramService.fixedPercentilePoints;
import static io.rainfall.store.hdr.HistogramService.roundTime;
import static io.rainfall.store.hdr.Percentile.MAX;
import static io.rainfall.store.hdr.Percentile.MEDIAN;
import static io.rainfall.store.hdr.Percentile._99;
import static io.rainfall.store.hdr.Percentile._99_99;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HistogramServiceTest {

  private static final String[][] SHUFFLED_RESOURCE_NAMES = new String[][] {
      { "149.hlog", "150.hlog", "152.hlog", "153.hlog" },
      { "153.hlog", "152.hlog", "150.hlog", "149.hlog" },
      { "150.hlog", "149.hlog", "153.hlog", "152.hlog" }
  };

  private final HistogramService histogramService = new HistogramService();

  @Test
  public void testReadHdrDataFull() {
    HdrData hdrData = readHlog("153.hlog", 2000);
    assertNotNull(hdrData);

    int size = 1802;
    assertThat(hdrData.size(), is(size));

    List<Long> startTimes = hdrData.getStartTimes();
    assertThat(startTimes.size(), is(size));
    assertThat(startTimes.get(0), is(1546881863987L));
    assertThat(startTimes.get(size - 1), is(1546883664000L));

    List<Double> tps = hdrData.getTps();
    assertThat(tps.size(), is(size));
    assertThat(tps.get(0), is(69000.0));
    assertThat(tps.get(size - 1), is(471.5284715284715));

    List<Double> means = hdrData.getMeans();
    assertThat(means.size(), is(size));
    assertThat(means.get(0), is(2.7547834037267083));
    assertThat(means.get(size - 1), is(0.6946302372881356));

    List<Double> errors = hdrData.getErrors();
    assertThat(errors.size(), is(size));
    assertThat(errors.get(0), is(2.7616774448249384));
    assertThat(errors.get(size - 1), is(0.4147095809479754));

    checkTimedPercentiles(hdrData);
    checkPercentiles(hdrData);
  }

  @Test
  public void testReadHdrDataReduced() {
    int maxDataPoints = 100;

    HdrData hdrData = readHlog("153.hlog", maxDataPoints);
    assertNotNull(hdrData);
    assertThat(hdrData.size(), is(maxDataPoints));

    List<Long> startTimes = hdrData.getStartTimes();
    assertThat(startTimes.size(), is(maxDataPoints));
    assertThat(startTimes.get(0), is(1546881863987L));
    assertThat(startTimes.get(maxDataPoints - 1), lessThan(1546883664000L));

    assertThat(hdrData.getTps().size(), is(maxDataPoints));
    assertThat(hdrData.getMeans().size(), is(maxDataPoints));
    assertThat(hdrData.getErrors().size(), is(maxDataPoints));

    checkTimedPercentiles(hdrData);
    checkPercentiles(hdrData);
  }

  private void checkTimedPercentiles(HdrData hdrData) {
    Percentile.all()
        .map(hdrData::getTimedPercentiles)
        .map(List::size)
        .forEach(size -> assertThat(size, is(hdrData.size())));

    IntStream.range(0, hdrData.size())
        .forEach(n -> {
          List<Double> actual = Percentile.all()
              .map(hdrData::getTimedPercentiles)
              .map(percentiles -> percentiles.get(n))
              .collect(toList());
          List<Double> ordered = actual.stream()
              .sorted()
              .collect(toList());
          assertThat(actual, is(ordered));
        });
  }

  private void checkPercentiles(HdrData hdrData) {
    assertThat(hdrData.getValueAtPercentile(MEDIAN), is(2074623L));
    assertThat(hdrData.getValueAtPercentile(_99), is(2795519L));
    assertThat(hdrData.getValueAtPercentile(_99_99), is(19300351L));
    assertThat(hdrData.getValueAtPercentile(MAX), is(34701311L));

    int numPercentiles = 126;

    List<Double> points = hdrData.getPercentilePoints();
    assertThat(points.size(), is(numPercentiles));
    assertThat(points.get(0), is(0.0));
    assertThat(points.get(numPercentiles - 1), is(1.0));

    List<Long> percentileValues = hdrData.getPercentileValues();
    assertThat(percentileValues.size(), is(numPercentiles));
    assertThat(percentileValues.get(0), is(407039L));
    assertThat(percentileValues.get(numPercentiles - 1), is(34701311L));

    assertThat(hdrData.getFixedPercentileValues().length, is(10));
  }

  @Test
  public void testBlankLog() {
    HdrData hdrData = histogramService.readHdrData(this::blankInputStream, 2000);
    testBlankData(hdrData);
  }

  @Test(expected = RuntimeException.class)
  public void testEmptyIntervalLog() {
    readHlog("emptyInterval.hlog", 1);
  }

  private ByteArrayInputStream blankInputStream() {
    return new ByteArrayInputStream(new byte[] {});
  }

  private void testBlankData(HdrData hdrData) {
    assertNotNull(hdrData);

    assertThat(hdrData.size(), is(0));
    assertThat(hdrData.getStartTimes().size(), is(0));
    assertThat(hdrData.getTps().size(), is(0));
    assertThat(hdrData.getMeans().size(), is(0));
    assertThat(hdrData.getErrors().size(), is(0));

    assertThat(hdrData.getValueAtPercentile(MEDIAN), is(0L));
    assertThat(hdrData.getValueAtPercentile(_99), is(0L));
    assertThat(hdrData.getValueAtPercentile(_99_99), is(0L));
    assertThat(hdrData.getValueAtPercentile(MAX), is(0L));

    assertThat(hdrData.getPercentilePoints().size(), is(0));
    assertThat(hdrData.getPercentileValues().size(), is(0));

    assertThat(hdrData.getFixedPercentileValues().length, is(10));

    checkTimedPercentiles(hdrData);
  }

  @Test
  public void testRepeatingDataPoint() {
    HdrData full = readHlog("repeat.hlog", 2);
    assertThat(full.size(), is(2));

    HdrData reduced = readHlog("repeat.hlog", 1);
    assertThat(reduced.size(), is(1));

    assertThat(full.getTps().get(0), is(full.getTps().get(1)));

    double tps = reduced.getTps().get(0);
    assertThat(full.getTps().get(0), is(tps));
    assertThat(full.getTps().get(1), is(tps));

    double mean = reduced.getMeans().get(0);
    assertThat(full.getMeans().get(0), is(mean));
    assertThat(full.getMeans().get(1), is(mean));

    double error = reduced.getErrors().get(0);
    assertThat(full.getErrors().get(0), is(error));
    assertThat(full.getErrors().get(1), is(error));

    assertEquals(full.getPercentilePoints(), reduced.getPercentilePoints());
    assertEquals(full.getPercentileValues(), reduced.getPercentileValues());
    Percentile.all()
        .forEach(p -> assertEquals(full.getValueAtPercentile(p), reduced.getValueAtPercentile(p)));

    checkTimedPercentiles(full);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroDataPoints() {
    histogramService.readHdrData(() -> mock(InputStream.class), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeDataPoints() {
    histogramService.readHdrData(() -> mock(InputStream.class), -1);
  }

  @Test
  public void testAggregateHdrDataEmptyList() {
    HdrData hdrData = histogramService.aggregateHdrData(emptyList());
    testBlankData(hdrData);
  }

  @Test(expected = RuntimeException.class)
  public void testAggregateHdrDataBlankLog() {
    List<Supplier<InputStream>> inputStreams = asList(
        () -> getResourceAsStream("153.hlog"),
        this::blankInputStream
    );
    histogramService.aggregateHdrData(inputStreams);
  }

  @Test
  public void testAggregateHdrDataMissingPointsAtStart() {
    List<Supplier<InputStream>> suppliers = hlogStream("105.hlog", "106.hlog", "109.hlog", "111.hlog")
        .collect(toList());
    HdrData hdrData = histogramService.aggregateHdrData(suppliers);
    assertNotNull(hdrData);
    assertThat(hdrData.size(), is(59));
    checkTimedPercentiles(hdrData);
  }

  @Test
  public void testAggregateHdrData() {
    Stream.of(SHUFFLED_RESOURCE_NAMES)
        .forEach(this::testAggregateHdrData);
  }

  /**
   * Test that no OOME is thrown.
   */
  @Test
  public void testAggregateHdrDataInParallel() {
    IntStream.range(0, 4)
        .parallel()
        .mapToObj(n -> SHUFFLED_RESOURCE_NAMES)
        .flatMap(Stream::of)
        .forEach(this::testAggregateHdrData);
  }

  private void testAggregateHdrData(String... resourceNames) {
    List<Supplier<InputStream>> suppliers = hlogStream(resourceNames)
        .collect(toList());
    HdrData hdrData = histogramService.aggregateHdrData(suppliers, Integer.MAX_VALUE);
    assertNotNull(hdrData);
    int size = 1798;
    assertThat(hdrData.size(), is(size));

    List<Long> startTimes = hdrData.getStartTimes();
    assertThat(startTimes.size(), is(size));
    assertThat(startTimes.get(0), is(1546881863000L));
    assertThat(startTimes.get(size - 1), is(1546883660000L));

    List<Double> tps = hdrData.getTps();
    assertThat(tps.size(), is(size));
    assertThat(tps.get(0), is(58000.999000999));
    assertThat(tps.get(size - 1), is(56311.75298804781));

    List<Double> means = hdrData.getMeans();
    assertThat(means.size(), is(size));
    assertThat(means.get(0), is(1.628546621333471));
    assertThat(means.get(size - 1), is(2.0790537436369103));

    List<Double> errors = hdrData.getErrors();
    assertThat(errors.size(), is(size));
    assertThat(errors.get(0), is(1.0412892932952253));
    assertThat(errors.get(size - 1), is(0.873150940271226));

    checkTimedPercentiles(hdrData);
    checkAggregatePercentiles(hdrData);
  }

  @Test
  public void testAggregateHdrDataReduced() {
    Stream.of(SHUFFLED_RESOURCE_NAMES)
        .forEach(this::testAggregateHdrDataReduced);
  }

  private void testAggregateHdrDataReduced(String... resourceNames) {
    List<Supplier<InputStream>> suppliers = hlogStream(resourceNames)
        .collect(toList());
    HdrData hdrData = histogramService.aggregateHdrData(suppliers, 100);
    assertNotNull(hdrData);
    int size = 100;
    assertThat(hdrData.size(), is(size));
    Stream.of(hdrData.getStartTimes(),
        hdrData.getTps(),
        hdrData.getMeans(),
        hdrData.getErrors())
        .map(List::size)
        .forEach(n -> assertThat(n, is(size)));

    checkAggregatePercentiles(hdrData);
  }

  private void checkAggregatePercentiles(HdrData hdrData) {
    int numPercentiles = 136;

    List<Double> points = hdrData.getPercentilePoints();
    assertThat(points.size(), is(numPercentiles));
    assertThat(points.get(0), is(0.0));
    assertThat(points.get(numPercentiles - 1), is(1.0));

    List<Long> percentileValues = hdrData.getPercentileValues();
    assertThat(percentileValues.size(), is(numPercentiles));
    assertThat(percentileValues.get(0), is(416255L));
    assertThat(percentileValues.get(numPercentiles - 1), is(34701311L));

    assertThat(hdrData.getFixedPercentileValues().length, is(10));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAggregateWithZeroDataPoints() {
    List<Supplier<InputStream>> suppliers = singletonList(() -> mock(InputStream.class));
    histogramService.aggregateHdrData(suppliers, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAggregateWithNegativeDataPoints() {
    List<Supplier<InputStream>> suppliers = singletonList(() -> mock(InputStream.class));
    histogramService.aggregateHdrData(suppliers, -1);
  }

  @Test
  public void testAggregateInequalLengtsInFrame() {
    List<Supplier<InputStream>> suppliers = hlogStream("232.hlog", "231.hlog", "228.hlog", "227.hlog")
        .collect(toList());
    HdrData hdrData = histogramService.aggregateHdrData(suppliers);
    assertThat(hdrData.size(), is(54));
    checkTimedPercentiles(hdrData);
  }

  @Test
  public void testRoundTime() {
    assertThat(roundTime(1546881863987L), is(1546881863000L));
    assertThat(roundTime(1546883660000L), is(1546883660000L));
  }

  @Test
  public void testFixedPercentilePoints() {
    assertThat(
        fixedPercentilePoints(0).collect(toList()),
        empty()
    );

    assertThat(
        fixedPercentilePoints(1).collect(toList()),
        contains(0.0)
    );

    assertThat(
        fixedPercentilePoints(2).collect(toList()),
        contains(0.0, 0.5)
    );

    assertThat(
        fixedPercentilePoints(5).collect(toList()),
        contains(0.0, 0.5, 0.75, 0.875, 0.9375)
    );
  }

  private Stream<Supplier<InputStream>> hlogStream(String... resourceNames) {
    return of(resourceNames)
        .map(name -> () -> getResourceAsStream(name));

  }

  private HdrData readHlog(String resourceName, int maxDataPoints) {
    return histogramService.readHdrData(
        () -> getResourceAsStream(resourceName), maxDataPoints);
  }

  private InputStream getResourceAsStream(String name) {
    return HistogramServiceTest.class.getResourceAsStream(name);
  }
}
