package io.rainfall.store.data;


import static io.rainfall.store.data.CompressionFormat.ZIP;

public class ZipServiceTest extends CompressionServiceTest {

  @Override
  CompressionService createCompressor() {
    return new ZipService(ZIP);
  }
}
