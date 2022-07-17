package io.rainfall.store.core;

import java.util.Set;

/**
 * @author Aurelien Broszniowski
 */

public class MetricsLog {

  private final String label;
  private final String cloudType;
  private final String metrics;

  public MetricsLog(String label, String cloudType, String metrics) {
    this.label = label;
    this.cloudType = cloudType;
    this.metrics = metrics;
  }

  public String getLabel() {
    return label;
  }

  public String getCloudType() {
    return cloudType;
  }

  public String getMetrics() {
    return metrics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final MetricsLog that = (MetricsLog)o;

    if (cloudType != null ? !cloudType.equals(that.cloudType) : that.cloudType != null) return false;
    return metrics != null ? metrics.equals(that.metrics) : that.metrics == null;
  }

  @Override
  public int hashCode() {
    int result = cloudType != null ? cloudType.hashCode() : 0;
    result = 31 * result + (metrics != null ? metrics.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MetricsLog{" +
           "cloudType='" + cloudType + '\'' +
           ", metrics='" + metrics + '\'' +
           '}';
  }
}
