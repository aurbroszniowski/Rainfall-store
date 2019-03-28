package io.rainfall.store.core;

import java.util.Objects;

public class TestCase {

  private final String description;

  private TestCase(Builder builder) {
    description = builder.description;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestCase testCase = (TestCase)o;
    return Objects.equals(description, testCase.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description);
  }

  @Override
  public String toString() {
    return "TestCase{" +
           "description='" + description + '\'' +
           '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements io.rainfall.store.core.Builder<TestCase> {
    private String description;

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    @Override
    public TestCase build() {
      return new TestCase(this);
    }
  }
}
