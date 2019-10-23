package io.rainfall.store.dataset;

import io.rainfall.store.values.MonitorLog;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class MonitorLogDataset extends LogDataset<
    MonitorLog,
    MonitorLogRecord,
    MonitorLogRepository,
    RunRecord,
    RunRepository> {

  MonitorLogDataset(@NonNull MonitorLogRepository repository,
                    @NonNull RunRepository parentRepository,
                    @NonNull PayloadRepository payloadRepository) {
    super(repository, parentRepository, payloadRepository);
  }

  @Override
  MonitorLogRecord create(RunRecord parent, MonitorLog value, PayloadRecord payloadRecord) {
    return new MonitorLogRecord(parent, value, payloadRecord);
  }

  @Override
  void addChild(RunRecord parent, MonitorLogRecord child) {
    parent.addMonitorLog(child);
  }
}
