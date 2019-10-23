package io.rainfall.store.dataset;

import io.rainfall.store.data.Payload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PACKAGE)
class LogRecord<V, P extends Record<?>> extends ChildRecord<V, P> {

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "payload_id", nullable = false)
  @NonNull
  private PayloadRecord payloadRecord;

  LogRecord(P parent, V value, PayloadRecord payloadRecord) {
    super(parent, value);
    this.payloadRecord = payloadRecord;
  }

  public Record<Payload> getPayloadRecord() {
    return payloadRecord;
  }
}
