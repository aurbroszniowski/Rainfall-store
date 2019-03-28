package io.rainfall.store.data;


import static io.rainfall.store.data.CompressionFormat.LZ4;

public class Lz4ServiceTest extends CompressionServiceTest {

  @Override
  CompressionService createCompressor() {
    return new Lz4Service(LZ4);
  }
}
