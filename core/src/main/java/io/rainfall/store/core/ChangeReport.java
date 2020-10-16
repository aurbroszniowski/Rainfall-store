/*
 * Copyright (c) 2014-2020 Aurélien Broszniowski
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

package io.rainfall.store.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Report of a possible change of performance between
 * a run under question and a baseline run.
 * The changes are represented as p-values under the
 * given threshold, mapped to the corresponding operations.
 */
public class ChangeReport {

  private final Long baselineID;

  private final double threshold;

  private final Map<String, Double> pValues;

  public ChangeReport(double threshold) {
    this(null, threshold, Collections.emptyMap());
  }

  public ChangeReport(Long baselineID, double threshold, Map<String, Double> pValues) {
    this.baselineID = baselineID;
    this.threshold = threshold;
    this.pValues = pValues;
  }

  /**
   * Baseline ID if it exists.
   *
   * @return perf test baseline id
   */
  public Optional<Long> getBaselineID() {
    return Optional.of(baselineID);
  }

  /**
   * Threshold p-value.
   *
   * @return p-value.
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * P-values below the threshold, mapped to the
   * corresponding operations.
   *
   * @return map of operations and p-values.
   */
  public Map<String, Double> getPValues() {
    return pValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChangeReport that = (ChangeReport)o;
    return Double.compare(that.threshold, threshold) == 0 &&
           Objects.equals(baselineID, that.baselineID) &&
           Objects.equals(pValues, that.pValues);
  }

  @Override
  public int hashCode() {

    return Objects.hash(baselineID, threshold, pValues);
  }

  @Override
  public String toString() {
    return "ChangeReport{" +
           "baselineID=" + baselineID +
           ", threshold=" + threshold +
           ", pValues=" + pValues +
           '}';
  }
}
