package io.rainfall.store.data;

import java.io.IOException;

public abstract class CompressionService {

  private final CompressionFormat format;

  CompressionService(CompressionFormat format) {
    this.format = format;
  }

  CompressionFormat getFormat() {
    return format;
  }

  public Payload compress(byte[] bytes) throws IOException {
    return new Payload(compressBytes(bytes), format, bytes.length);
  }

  protected abstract byte[] compressBytes(byte[] bytes) throws IOException;

  public byte[] decompress(Payload payload) throws IOException {
    if (payload.getFormat() != this.format) {
      throw new IllegalArgumentException("Data format " + payload.getFormat() +
                                         " != CompressionService format " + format);
    }
    return decompressBytes(payload.getData(), payload.getOriginalLength());
  }

  protected abstract byte[] decompressBytes(byte[] bytes, int originalLength) throws IOException;
}
