package io.rainfall.store.dataset;

import io.rainfall.store.values.Run;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static io.rainfall.store.values.Run.Status;

@Component
public class RunDataset extends ChildDataset<Run, RunRecord, RunRepository, CaseRecord, CaseRepository> {

  @Autowired
  public RunDataset(@NonNull RunRepository repository, @NonNull CaseRepository parentRepository) {
    super(repository, parentRepository);
  }

  @Override
  RunRecord create(CaseRecord parent, Run value) {
    return new RunRecord(parent, value);
  }

  @Override
  void addChild(CaseRecord parent, RunRecord child) {
    parent.addRun(child);
  }

  public void setStatus(long id, Status status) {
    repository().setStatus(id, status);
  }

  public void setBaseline(long id, boolean baseline) {
    repository().setBaseline(id, baseline);
  }

  public Optional<Long> getLastBaselineID(long parentId) {
    return repository().getLastBaselineID(parentId);
  }

  public List<RunRecord> findByIds(long[] ids) {
    return repository().findByIds(ids);
  }
}
