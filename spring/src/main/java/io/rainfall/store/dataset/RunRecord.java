package io.rainfall.store.dataset;

import io.rainfall.store.values.Run;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "run")
@AttributeOverrides(@AttributeOverride(name = "value.version", column = @Column(length = 32)))
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class RunRecord extends ChildRecord<Run, CaseRecord> {

  public RunRecord(CaseRecord parent, Run value) {
    super(parent, value);
  }

  @JsonIgnore
  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL
  )
  private List<MonitorLogRecord> monitorLogs = new CopyOnWriteArrayList<>();

  public List<MonitorLogRecord> getMonitorLogs() {
    return Collections.unmodifiableList(monitorLogs);
  }

  void addMonitorLog(MonitorLogRecord logRecord) {
    monitorLogs.add(logRecord);
  }

  @JsonIgnore
  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL
  )
  private List<JobRecord> jobs = new CopyOnWriteArrayList<>();

  public List<JobRecord> getJobs() {
    return Collections.unmodifiableList(jobs);
  }

  void addJob(JobRecord job) {
    jobs.add(job);
  }
}
