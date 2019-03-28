package io.rainfall.store.core;

import java.util.Objects;

public class TestRun {

  public enum Status {
    UNKNOWN, INCOMPLETE, COMPLETE, FAILED
  }

  private final String version;
  private final String className;
  private final String checksum;
  private final Status status;
  private final boolean baseline;

  private TestRun(Builder builder) {
    this.version = builder.version;
    this.className = builder.className;
    this.checksum = builder.checksum;
    this.status = builder.status;
    this.baseline = builder.baseline;
  }

  public String getVersion() {
    return version;
  }

  public String getClassName() {
    return className;
  }

  public String getChecksum() {
    return checksum;
  }

  public boolean isBaseline() {
    return baseline;
  }

  /**
   * Handle null for backward compatibility.
   */
  public Status getStatus() {
    return status == null ? Status.INCOMPLETE : status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestRun testRun = (TestRun)o;
    return Objects.equals(version, testRun.version) &&
           Objects.equals(className, testRun.className) &&
           Objects.equals(checksum, testRun.checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, className, checksum);
  }

  @Override
  public String toString() {
    return "TestRun{" +
           "version='" + version + '\'' +
           ", className='" + className + '\'' +
           ", checksum='" + checksum + '\'' +
           '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements io.rainfall.store.core.Builder<TestRun> {

    private String version;
    private String className;
    private String checksum;
    private Status status = Status.INCOMPLETE;
    private boolean baseline;

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder className(String className) {
      this.className = className;
      return this;
    }

    public Builder checksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    public Builder baseline(boolean value) {
      this.baseline = value;
      return this;
    }

    public Builder status(String statusName) {
      //fixing typo error in previous version
      Status status = "INCOMPETE".equals(statusName)
          ? Status.INCOMPLETE
          : Status.valueOf(statusName);
      return status(status);
    }

    @Override
    public TestRun build() {
      return new TestRun(this);
    }
  }
}
