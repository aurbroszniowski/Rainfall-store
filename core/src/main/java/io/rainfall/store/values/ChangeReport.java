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

package io.rainfall.store.values;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Report of a possible change of performance between
 * a run under question and a baseline run.
 * The changes are represented as p-values under the
 * given threshold, mapped to the corresponding operations.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class ChangeReport {

  private final Long baselineID;

  private final double threshold;

  private final Map<String, Double> pValues;

  public ChangeReport(double threshold) {
    this(null, threshold, Collections.emptyMap());
  }


  /**
   * Baseline ID if it exists.
   *
   * @return optional baseline ID.
   */
  public Optional<Long> getBaselineID() {
    return Optional.ofNullable(baselineID);
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

}
