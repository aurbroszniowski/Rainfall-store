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
public class Case {

  @NonNull
  @Builder.Default
  private final String name = "No name";

  @NonNull
  @Builder.Default
  private final String description = "";
}