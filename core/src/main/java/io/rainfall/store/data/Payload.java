package io.rainfall.store.data;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import static io.rainfall.store.data.CompressionFormat.RAW;


public class Payload {

  static Charset CHARSET = Charset.forName("UTF-8");

  private final byte[] data;
  private final CompressionFormat format;
  private final int originalLength;

  public static Payload raw(String data) {
    return raw(data.getBytes(CHARSET));
  }

  public static Payload raw(byte[] data) {
    return new Payload(data, RAW, data.length);
  }

  public static Payload of(byte[] data, CompressionFormat format, int originalLength) {
    return new Payload(data, format, originalLength);
  }

  Payload(byte[] data, CompressionFormat format, int originalLength) {
    this.data = data;
    this.format = format;
    this.originalLength = originalLength;
  }

  public byte[] getData() {
    return data;
  }

  public CompressionFormat getFormat() {
    return format;
  }

  public int getOriginalLength() {
    return originalLength;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Payload payload = (Payload)o;
    return originalLength == payload.originalLength &&
           Arrays.equals(data, payload.data) &&
           format == payload.format;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(format, originalLength);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }

  @Override
  public String toString() {
    return "Payload{" +
           "format=" + format +
           ", originalLength=" + originalLength +
           '}';
  }

  public static String toUtfString(byte[] data) {
    return new String(data, CHARSET);
  }
}
