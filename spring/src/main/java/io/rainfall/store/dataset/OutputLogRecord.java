package io.rainfall.store.dataset;

import io.rainfall.store.values.OutputLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "output_log")
@AttributeOverrides({
    @AttributeOverride(name = "value.format", column = @Column(length = 32)),
    @AttributeOverride(name = "value.operation", column = @Column(length = 32))
})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OutputLogRecord extends LogRecord<OutputLog, JobRecord> {

  OutputLogRecord(JobRecord parent, OutputLog value, PayloadRecord payloadRecord) {
    super(parent, value, payloadRecord);
  }
}
