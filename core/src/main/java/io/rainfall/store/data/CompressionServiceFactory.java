package io.rainfall.store.data;

public class CompressionServiceFactory {

  public static CompressionService compressionService(CompressionFormat format) {
    switch (format) {
      case RAW:
        return new RawDataService(format);
      case ZIP:
        return new ZipService(format);
      case LZ4:
        return new Lz4Service(format);
      default:
        throw new IllegalArgumentException("Unsupported compression format: " + format);
    }
  }
}
