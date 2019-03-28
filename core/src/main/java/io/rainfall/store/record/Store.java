package io.rainfall.store.record;

import java.util.Set;

public interface Store
    extends StoreWriter, StoreReader, AutoCloseable {

  Set<String> getOperationsForRun(long runId);

  boolean setBaseline(long runId, boolean value);
}
