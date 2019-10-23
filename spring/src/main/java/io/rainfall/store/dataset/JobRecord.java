package io.rainfall.store.dataset;

import io.rainfall.store.values.Job;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "job")
@AttributeOverride(name = "value.details", column = @Column(length = 1024))
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class JobRecord extends ChildRecord<Job, RunRecord> {

  JobRecord(RunRecord parent, Job value) {
    super(parent, value);
  }

  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL
  )
  private List<OutputLogRecord> outputLogs = new CopyOnWriteArrayList<>();

  public List<OutputLogRecord> getOutputLogs() {
    return Collections.unmodifiableList(outputLogs);
  }

  void addOutputLog(OutputLogRecord job) {
    outputLogs.add(job);
  }
}
