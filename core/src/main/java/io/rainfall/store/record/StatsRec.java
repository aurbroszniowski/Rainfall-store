package io.rainfall.store.record;


import io.rainfall.store.core.StatsLog;

public class StatsRec extends ChildRec<Long, Long, StatsLog> {

  public StatsRec(Long parentID, Long ID, StatsLog value, long timeStamp) {
    super(parentID, ID, value, timeStamp);
  }
}
