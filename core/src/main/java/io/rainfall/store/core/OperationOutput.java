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

package io.rainfall.store.core;


import io.rainfall.store.data.Payload;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class OperationOutput extends FileOutput {

  private final String format;
  private final String operation;

  private OperationOutput(Builder builder) {
    super(builder);
    format = builder.format;
    operation = builder.operation;
  }

  public String getFormat() {
    return format;
  }

  public String getOperation() {
    return operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    OperationOutput output = (OperationOutput)o;
    return Objects.equals(format, output.format) &&
           Objects.equals(operation, output.operation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), format, operation);
  }

  @Override
  public String toString() {
    return "OperationOutput{" +
           "format='" + format + '\'' +
           ", operation='" + operation + '\'' +
           "}; " + super.toString();
  }

  public OperationOutput unloaded() {
    return withPayload(null);
  }

  public OperationOutput withPayload(Payload payload) {
    return builder()
        .payload(payload)
        .format(format)
        .operation(operation)
        .build();
  }

  public static Set<String> allFormats() {
    return Collections.singleton("hlog");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends FileOutput.Builder<OperationOutput, Builder> {

    private String format = "hlog";
    private String operation;

    public Builder format(String format) {
      this.format = format;
      return this;
    }

    public Builder operation(String operation) {
      this.operation = operation;
      return this;
    }

    @Override
    public OperationOutput build() {
      return new OperationOutput(this);
    }
  }
}
