package io.rainfall.store.dataset;

import lombok.NonNull;

import java.util.List;

abstract class ChildDataset<
    V,
    CR extends ChildRecord<V, PR>,
    CS extends ChildRepository<CR>,
    PR extends Record<?>,
    PS extends RecordRepository<PR>
    > extends Dataset<CR, CS> {

  @NonNull
  private final PS parentRepository;

  ChildDataset(@NonNull CS repository, @NonNull PS parentRepository) {
    super(repository);
    this.parentRepository = parentRepository;
  }

  public CR save(long parentId, V value) {
    PR parent = parentRepository.findById(parentId)
        .orElseThrow(() -> new IllegalArgumentException("Parent ID not found: " + parentId));
    CR created = create(parent, value);
    CR saved = saveRecord(created);
    addChild(parent, saved);
    parentRepository.save(parent);
    return saved;
  }

  abstract CR create(PR parent, V value);

  abstract void addChild(PR parent, CR child);

  public List<CR> findByParentId(long parentId) {
    return repository().findByParentId(parentId);
  }
}
