package io.rainfall.store.core;

import java.util.Objects;

public class StatsLog extends FileOutput {

  private final String host;
  private final String type;

  private StatsLog(Builder builder) {
    super(builder);
    host = builder.host;
    type = builder.type;
  }

  public String getHost() {
    return host;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    StatsLog statsLog = (StatsLog)o;
    return Objects.equals(host, statsLog.host) &&
           Objects.equals(type, statsLog.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), host, type);
  }

  @Override
  public String toString() {
    return "StatsLog{" +
           "host='" + host + '\'' +
           ", type='" + type + '\'' +
           "}; " + super.toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends FileOutput.Builder<StatsLog, Builder> {

    private String host;
    private String type = "vmstat";

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    @Override
    public StatsLog build() {
      return new StatsLog(this);
    }
  }
}
