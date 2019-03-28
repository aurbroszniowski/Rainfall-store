package io.rainfall.store.record;


import io.rainfall.store.core.TestCase;

public class TestCaseRec extends Rec<String, TestCase> {
  public TestCaseRec(String uniqueName, TestCase value, long timeStamp) {
    super(uniqueName, value, timeStamp);
  }
}
