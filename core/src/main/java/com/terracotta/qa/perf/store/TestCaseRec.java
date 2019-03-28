package com.terracotta.qa.perf.store;

import com.terracotta.qa.perf.core.TestCase;

public class TestCaseRec extends Rec<String, TestCase> {
  public TestCaseRec(String uniqueName, TestCase value, long timeStamp) {
    super(uniqueName, value, timeStamp);
  }
}
