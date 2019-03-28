package io.rainfall.store.data;

import org.junit.Test;

import static io.rainfall.store.data.CompressionFormat.LZ4;
import static io.rainfall.store.data.CompressionFormat.RAW;
import static io.rainfall.store.data.CompressionFormat.ZIP;
import static io.rainfall.store.data.CompressionServiceFactory.compressionService;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class CompressionServiceFactoryTest {

  @Test
  public void testZip() {
    CompressionService zipService = compressionService(ZIP);
    assertThat(zipService, instanceOf(ZipService.class));
  }

  @Test
  public void testLz4() {
    CompressionService lz4Service = compressionService(LZ4);
    assertThat(lz4Service, instanceOf(Lz4Service.class));
  }

  @Test
  public void testRaw() {
    CompressionService noop = compressionService(RAW);
    assertThat(noop, instanceOf(RawDataService.class));
  }
}
