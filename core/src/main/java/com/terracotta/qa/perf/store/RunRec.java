package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.TestRun;

public class RunRec extends ChildRec<String, Long, TestRun> {

  public RunRec(String testName, Long key, TestRun value, long timeStamp) {
    super(testName, key, value, timeStamp);
  }
}
