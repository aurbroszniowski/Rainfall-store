package io.rainfall.store.dataset;

import io.rainfall.store.values.Case;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CaseDataset extends Dataset<CaseRecord, CaseRepository> {

  @Autowired
  CaseDataset(@NonNull CaseRepository repository) {
    super(repository);
  }

  public CaseRecord save(Case value) {
    CaseRecord record = new CaseRecord(value);
    return saveRecord(record);
  }

  public void setDescription(long id, String description) {
    repository().setDescription(id, description);
  }

  public Optional<CaseRecord> findByName(String caseName) {
    return repository().findByValueName(caseName);
  }
}
