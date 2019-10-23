package io.rainfall.store.dataset;

import io.rainfall.store.values.MonitorLog;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

  public List<MonitorLogRecord> findMonitorLogsForRun(long parentId) {
    return repository().findByParentId(parentId);
  }

  public List<MonitorLogRecord> findMonitorLogsForRunAndHost(long parentId, String host) {
    return repository().findByParentIdAndValueHost(parentId, host);
  }

  public Optional<PayloadRecord> getPayload(long id) {
    return repository().getPayload(id);
  }
}
