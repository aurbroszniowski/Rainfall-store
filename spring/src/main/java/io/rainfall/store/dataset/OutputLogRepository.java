package io.rainfall.store.dataset;

import io.rainfall.store.data.Payload;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

interface OutputLogRepository extends ChildRepository<OutputLogRecord> {

  @Query(
      value = "select distinct r.value.operation " +
              "from OutputLogRecord r " +
              "inner join JobRecord j on r.parent.id = j.id " +
              "where j.parent.id = :runId"
  )
  List<String> findOperationsByRunId(@Param("runId") long runId);

  @Query(
      value = "select r.payloadRecord.value " +
              "from OutputLogRecord r " +
              "inner join JobRecord j on r.parent.id = j.id " +
              "where j.parent.id = :runId and r.value.operation = :op"
  )
  List<Payload> findOutputLogsByRunIdAndOperation(@Param("runId") long runId, @Param("op") String operation);

  /**
   * select distinct operation
   * from output_log ol
   * inner join job j on ol.parent_id = j.id
   * where j.parent_id in (:runIds)
   * group by operation
   * having count(distinct j.parent_id) = :size;
   */
  @Query(
      value = "select distinct r.value.operation " +
              "from OutputLogRecord r " +
              "inner join JobRecord j on r.parent.id = j.id " +
              "where j.parent.id in :runIds " +
              "group by r.value.operation " +
              "having count(distinct j.parent.id) = :size"
  )
  Set<String> findCommonOperationsForRuns(@Param("runIds") long[] runIds,
                                          @Param("size") long size);
}
