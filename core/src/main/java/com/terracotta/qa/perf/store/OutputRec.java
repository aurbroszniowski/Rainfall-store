package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.OperationOutput;

public class OutputRec extends ChildRec<Long, Long, OperationOutput> {

  public OutputRec(Long parentID, Long ID, OperationOutput value, long timeStamp) {
    super(parentID, ID, value, timeStamp);
  }
}
