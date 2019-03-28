package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.ClientJob;

public class ClientJobRec extends ChildRec<Long, Long, ClientJob> {

  public ClientJobRec(Long parentID, Long ID, ClientJob value, long timeStamp) {
    super(parentID, ID, value, timeStamp);
  }
}
