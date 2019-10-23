package io.rainfall.store.dataset;

import io.rainfall.store.values.OutputLog;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class OutputLogDataset extends LogDataset<
    OutputLog,
    OutputLogRecord,
    OutputLogRepository,
    JobRecord,
    JobRepository> {

  OutputLogDataset(@NonNull OutputLogRepository repository,
                   @NonNull JobRepository parentRepository,
                   @NonNull PayloadRepository payloadRepository) {
    super(repository, parentRepository, payloadRepository);
  }

  @Override
  OutputLogRecord create(JobRecord parent, OutputLog value, PayloadRecord payloadRecord) {
    return new OutputLogRecord(parent, value, payloadRecord);
  }

  @Override
  void addChild(JobRecord parent, OutputLogRecord child) {
    parent.addOutputLog(child);
  }
}
