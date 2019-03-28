package io.rainfall.store.data;

import org.junit.Test;

import java.io.IOException;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

abstract class CompressionServiceTest {

  private final CompressionService compressionService = createCompressor();

  abstract CompressionService createCompressor();

  @Test
  public void testData() throws IOException {
    String path = CompressionServiceTest.class
        .getResource("/" + "GET.hlog")
        .getPath();
    byte[] bytes = readAllBytes(get(path));
    Payload payload = compressionService.compress(bytes);
    assertEquals(bytes.length, payload.getOriginalLength());
    byte[] decompressed = compressionService.decompress(payload);
    assertArrayEquals(bytes, decompressed);
  }
}

