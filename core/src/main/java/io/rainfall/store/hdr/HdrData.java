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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class HdrData {

  private final List<Long> startTimes;
  private final List<Double> tps;
  private final List<Double> errors;
  private final List<Double> means;
  private final Map<Percentile, List<Double>> timedPercentiles;
  private final Map<Percentile, Long> roundedPercentiles;
  private final List<Double> percentilePoints;
  private final List<Long> percentileValues;
  private final List<Long> fixedPercentileValues;

  private HdrData(Builder builder) {
    this.startTimes = unmodifiableList(builder.startTimes);
    this.tps = unmodifiableList(builder.tps);
    this.errors = unmodifiableList(builder.errors);
    this.means = unmodifiableList(builder.means);
    this.timedPercentiles = unmodifiableMap(builder.timedPercentiles);
    this.roundedPercentiles = unmodifiableMap(builder.roundedPercentiles);
    this.percentilePoints = unmodifiableList(builder.percentilePoints);
    this.percentileValues = unmodifiableList(builder.percentileValues);
    this.fixedPercentileValues = unmodifiableList(builder.fixedPercentileValues);
  }

  public int size() {
    return startTimes.size();
  }

  public List<Long> getStartTimes() {
    return startTimes;
  }

  public List<Double> getTps() {
    return tps;
  }

  public List<Double> getTimedPercentiles(Percentile percentile) {
    return timedPercentiles.getOrDefault(percentile, emptyList());
  }

  public List<Double> getMeans() {
    return means;
  }

  public List<Double> getErrors() {
    return errors;
  }

  public long getValueAtPercentile(Percentile percentile) {
    return roundedPercentiles.get(percentile);
  }

  public List<Double> getPercentilePoints() {
    return percentilePoints;
  }

  public List<Long> getPercentileValues() {
    return percentileValues;
  }

  public double[] getFixedPercentileValues() {
    return fixedPercentileValues.stream()
        .mapToDouble(n -> (double)n)
        .toArray();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HdrData hdrData = (HdrData)o;
    return Objects.equals(startTimes, hdrData.startTimes) &&
           Objects.equals(tps, hdrData.tps) &&
           Objects.equals(errors, hdrData.errors) &&
           Objects.equals(means, hdrData.means) &&
           Objects.equals(timedPercentiles, hdrData.timedPercentiles) &&
           Objects.equals(roundedPercentiles, hdrData.roundedPercentiles) &&
           Objects.equals(percentilePoints, hdrData.percentilePoints) &&
           Objects.equals(percentileValues, hdrData.percentileValues) &&
           Objects.equals(fixedPercentileValues, hdrData.fixedPercentileValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTimes, tps, errors, means, timedPercentiles, roundedPercentiles, percentilePoints, percentileValues, fixedPercentileValues);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<Long> startTimes = new ArrayList<>();
    private final List<Double> tps = new ArrayList<>();
    private final List<Double> means = new ArrayList<>();
    private final List<Double> errors = new ArrayList<>();
    private final EnumMap<Percentile, List<Double>> timedPercentiles = new EnumMap<>(Percentile.class);
    private final EnumMap<Percentile, Long> roundedPercentiles = new EnumMap<>(Percentile.class);
    private final List<Double> percentilePoints = new ArrayList<>();
    private final List<Long> percentileValues = new ArrayList<>();
    private final List<Long> fixedPercentileValues = new ArrayList<>();

    public Builder addStartTime(long startTime) {
      this.startTimes.add(startTime);
      return this;
    }

    public Builder addTps(double tps) {
      this.tps.add(tps);
      return this;
    }

    public Builder addTimedPercentile(Percentile percentile, double value) {
      this.timedPercentiles.computeIfAbsent(percentile, p -> new ArrayList<>())
          .add(value);
      return this;
    }

    public Builder addMean(double mean) {
      this.means.add(mean);
      return this;
    }

    public Builder addError(double error) {
      errors.add(error);
      return this;
    }

    public Builder roundedPercentiles(Map<Percentile, Long> roundedPercentiles) {
      this.roundedPercentiles.putAll(roundedPercentiles);
      return this;
    }

    public Builder addPercentile(double point, long value) {
      this.percentilePoints.add(point);
      this.percentileValues.add(value);
      return this;
    }

    public Builder fixedPercentileValues(List<Long> values) {
      this.fixedPercentileValues.addAll(values);
      return this;
    }

    public HdrData build() {
      return new HdrData(this);
    }
  }
}
