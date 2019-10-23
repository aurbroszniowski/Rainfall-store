package io.rainfall.store.values;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Run {

  public enum Status {
    UNKNOWN, INCOMPLETE, COMPLETE, FAILED
  }

  @Builder.Default
  @NonNull
  private final String version = "";

  @Builder.Default
  @NonNull
  private final String className = "";

  @Builder.Default
  @NonNull
  private final String checksum = "";

  @Builder.Default
  @NonNull
  private final Status status = Status.UNKNOWN;

  @Builder.Default
  private final boolean baseline = false;
}
