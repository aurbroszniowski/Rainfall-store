package io.rainfall.store.values;

import io.rainfall.store.data.Payload;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MonitorLog extends Log {

  private final String host;

  private final String type;

  @SuppressWarnings("unused")
  private MonitorLog() {
    this(null, "", "");
  }

  @Builder
  private MonitorLog(Payload payload, String host, String type) {
    super(payload);
    this.host = host;
    this.type = type;
  }
}
