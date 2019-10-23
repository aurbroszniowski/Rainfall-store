package io.rainfall.store.dataset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@NoRepositoryBean
public abstract class Dataset<R extends Record<?>, S extends RecordRepository<R>> {

  private final S repository;

  @Autowired
  Dataset(S repository) {
    this.repository = repository;
  }

  R saveRecord(R record) {
    return repository.save(record);
  }

  public Optional<R> getRecord(Long aLong) {
    return repository.findById(aLong);
  }

  public Iterable<R> getRecords() {
    return repository.findAll();
  }

  S repository() {
    return repository;
  }
}
