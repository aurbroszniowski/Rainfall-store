package io.rainfall.store.values;

import io.rainfall.store.data.Payload;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class Log {

  @Getter
  private final Payload payload;
}
