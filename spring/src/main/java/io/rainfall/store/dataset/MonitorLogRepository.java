package io.rainfall.store.dataset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface MonitorLogRepository extends ChildRepository<MonitorLogRecord> {

  @Query(value = "select r from MonitorLogRecord r where r.value.host = :host and r.parent.id = :parentId")
  List<MonitorLogRecord> findByParentIdAndValueHost(@Param("parentId") long parentId, @Param("host") String host);

  @Query(value = "select r.payloadRecord from MonitorLogRecord r where r.id = :id")
  Optional<PayloadRecord> getPayload(@Param("id") long id);
}
