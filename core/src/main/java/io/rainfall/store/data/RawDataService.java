package io.rainfall.store.data;

class RawDataService extends CompressionService {

  RawDataService(CompressionFormat format) {
    super(format);
  }

  @Override
  protected byte[] compressBytes(byte[] bytes) {
    return bytes;
  }

  @Override
  protected byte[] decompressBytes(byte[] bytes, int originalLength) {
    return bytes;
  }
}
