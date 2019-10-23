package io.rainfall.store.dataset;

import io.rainfall.store.values.MonitorLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "monitor_log")
@AttributeOverride(name = "value.type", column = @Column(length = 32))
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MonitorLogRecord extends LogRecord<MonitorLog, RunRecord> {

  MonitorLogRecord(RunRecord parent, MonitorLog value, PayloadRecord payloadRecord) {
    super(parent, value, payloadRecord);
  }
}
