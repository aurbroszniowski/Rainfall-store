package io.rainfall.store.data;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

class Lz4Service extends CompressionService {

  private static final LZ4Factory FACTORY = LZ4Factory.fastestInstance();

  private final LZ4Compressor compressor = FACTORY.fastCompressor();
  private final LZ4FastDecompressor decompressor = FACTORY.fastDecompressor();

  Lz4Service(CompressionFormat format) {
    super(format);
  }

  @Override
  protected byte[] compressBytes(byte[] bytes) {
    return compressor.compress(bytes);
  }

  @Override
  protected byte[] decompressBytes(byte[] bytes, int originalLength) {
    return decompressor.decompress(bytes, originalLength);
  }
}
