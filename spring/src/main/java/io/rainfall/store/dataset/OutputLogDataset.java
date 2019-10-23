package io.rainfall.store.dataset;

import io.rainfall.store.data.Payload;
import io.rainfall.store.values.OutputLog;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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

  public List<OutputLogRecord> findOutputLogsByRunId(long parentId) {
    return repository().findByParentId(parentId);
  }

  public List<String> findOperationsByRunId(long runId) {
    return repository().findOperationsByRunId(runId);
  }

  public List<Payload> findOutputLogsByRunIdAndOperation(long runId, String operation) {
    return repository().findOutputLogsByRunIdAndOperation(runId, operation);
  }

  public Set<String> findCommonOperationsForRuns(long... runIds) {
    return repository().findCommonOperationsForRuns(runIds, runIds.length);
  }
}
