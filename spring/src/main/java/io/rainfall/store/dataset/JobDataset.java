package io.rainfall.store.dataset;

import io.rainfall.store.values.Job;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class JobDataset extends ChildDataset<
    Job,
    JobRecord,
    JobRepository,
    RunRecord,
    RunRepository> {


  JobDataset(@NonNull JobRepository repository, @NonNull RunRepository parentRepository) {
    super(repository, parentRepository);
  }

  @Override
  JobRecord create(RunRecord parent, Job value) {
    return new JobRecord(parent, value);
  }

  @Override
  void addChild(RunRecord parent, JobRecord child) {
    parent.addJob(child);
  }
}
