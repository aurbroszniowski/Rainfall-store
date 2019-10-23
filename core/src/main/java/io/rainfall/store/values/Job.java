package io.rainfall.store.values;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Job {

  @Builder.Default
  private final int clientNumber = -1;

  @Builder.Default
  private final String host = "";

  @Builder.Default
  private final String symbolicName = "";

  @Builder.Default
  private final String details = "";
}
