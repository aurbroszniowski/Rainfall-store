package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.StatsLog;

public class StatsRec extends ChildRec<Long, Long, StatsLog> {

  public StatsRec(Long parentID, Long ID, StatsLog value, long timeStamp) {
    super(parentID, ID, value, timeStamp);
  }
}
