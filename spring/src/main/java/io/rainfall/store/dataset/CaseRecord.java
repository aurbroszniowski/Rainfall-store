package io.rainfall.store.dataset;

import io.rainfall.store.values.Case;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
@Table(name = "test_case")
@AttributeOverrides({
    @AttributeOverride(name = "value.name", column = @Column(unique = true)),
    @AttributeOverride(name = "value.description", column = @Column(length = 1024))
})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CaseRecord extends Record<Case> {

  CaseRecord(Case value) {
    super(value);
  }

  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL
  )
  private List<RunRecord> runs = new CopyOnWriteArrayList<>();

  public List<RunRecord> getRuns() {
    return Collections.unmodifiableList(runs);
  }

  void addRun(RunRecord run) {
    runs.add(run);
  }
}
