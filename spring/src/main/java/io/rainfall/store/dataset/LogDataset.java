package io.rainfall.store.dataset;

import io.rainfall.store.values.Log;
import lombok.NonNull;


abstract class LogDataset<
    V extends Log,
    CR extends LogRecord<V, PR>,
    CS extends ChildRepository<CR>,
    PR extends Record<?>,
    PS extends RecordRepository<PR>
    > extends ChildDataset<V, CR, CS, PR, PS> {

  @NonNull
  private final PayloadRepository payloadRepository;

  LogDataset(@NonNull CS repository, @NonNull PS parentRepository,
             @NonNull PayloadRepository payloadRepository) {
    super(repository, parentRepository);
    this.payloadRepository = payloadRepository;
  }

  @Override
  CR create(PR parent, V value) {
    PayloadRecord payloadRecord = new PayloadRecord(value.getPayload());
    payloadRepository.save(payloadRecord);
    return create(parent, value, payloadRecord);
  }

  abstract CR create(PR parent, V value, PayloadRecord payloadRecord);
}
