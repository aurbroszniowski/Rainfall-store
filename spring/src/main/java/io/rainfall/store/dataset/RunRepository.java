package io.rainfall.store.dataset;

import io.rainfall.store.values.Run;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface RunRepository extends ChildRepository<RunRecord> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update RunRecord r set r.value.status = :status where r.id = :id")
  void setStatus(@Param("id") long id, @Param("status") Run.Status status);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update RunRecord r set r.value.baseline = :baseline where r.id = :id")
  void setBaseline(@Param("id") long id, @Param("baseline") boolean baseline);

  @Query(value = "select max(id) from RunRecord r where " +
                 "r.value.baseline = true and r.parent.id = :parentId")
  Optional<Long> getLastBaselineID(@Param("parentId") long parentId);

  @Query(value = "select r from RunRecord r where r.id in :ids")
  List<RunRecord> findByIds(@Param("ids") long[] ids);
}
