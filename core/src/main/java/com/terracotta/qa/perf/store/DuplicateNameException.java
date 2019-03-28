package com.terracotta.qa.perf.store;

public class DuplicateNameException extends IllegalArgumentException {
  public DuplicateNameException(String uniqueName) {
    super("Unique name already exists: " + uniqueName + ".");
  }
}
