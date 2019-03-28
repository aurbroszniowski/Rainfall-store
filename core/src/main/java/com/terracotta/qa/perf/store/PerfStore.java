package com.terracotta.qa.perf.store;

import java.util.Set;

public interface PerfStore
    extends PerfStoreWriter, PerfStoreReader, AutoCloseable {

  Set<String> getOperationsForRun(long runId);

  boolean setBaseline(long runId, boolean value);
}
