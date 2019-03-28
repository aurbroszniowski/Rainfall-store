package io.rainfall.store.record;

public class DuplicateNameException extends IllegalArgumentException {
  public DuplicateNameException(String uniqueName) {
    super("Unique name already exists: " + uniqueName + ".");
  }
}
