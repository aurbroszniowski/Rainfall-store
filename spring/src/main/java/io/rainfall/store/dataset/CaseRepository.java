package io.rainfall.store.dataset;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CaseRepository extends RecordRepository<CaseRecord> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update CaseRecord r set r.value.description = :description where r.id = :id")
  void setDescription(@Param("id") long id, @Param("description") String description);
}
