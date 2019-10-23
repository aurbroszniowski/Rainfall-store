package io.rainfall.store.dataset;

import io.rainfall.store.data.Payload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "payload")
@AttributeOverrides({
    @AttributeOverride(name = "value.format", column = @Column(name = "compression_format")),
    @AttributeOverride(name = "value.data", column = @Column(columnDefinition = "BLOB"))
})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PayloadRecord extends Record<Payload> {

  PayloadRecord(Payload value) {
    super(value);
  }
}
