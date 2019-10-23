package io.rainfall.store.values;

import io.rainfall.store.data.Payload;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OutputLog extends Log {

  private final String format;

  private final String operation;

  @SuppressWarnings("unused")
  private OutputLog() {
    this(null, "", "");
  }

  @Builder
  private OutputLog(Payload payload, String format, String operation) {
    super(payload);
    this.format = format;
    this.operation = operation;
  }
}
