package io.rainfall.store.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static io.rainfall.store.data.Payload.CHARSET;
import static java.util.stream.Collectors.joining;

class ZipService extends CompressionService {

  ZipService(CompressionFormat format) {
    super(format);
  }

  @Override
  public byte[] compressBytes(byte[] bytes) throws IOException {
    try (
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, CHARSET));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zs = new ZipOutputStream(baos);
        OutputStreamWriter ow = new OutputStreamWriter(zs);
        BufferedWriter bw = new BufferedWriter(ow)
    ) {
      ZipEntry entry = new ZipEntry("1");
      zs.putNextEntry(entry);
      br.lines().forEach(line -> writeLine(bw, line));
      bw.flush();
      zs.closeEntry();
      return baos.toByteArray();
    }
  }

  private static void writeLine(BufferedWriter bw, String line) {
    try {
      bw.write(line);
      bw.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected byte[] decompressBytes(byte[] bytes, int originalLength) throws IOException {
    try (
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ZipInputStream zs = new ZipInputStream(bais);
        BufferedReader br = new BufferedReader(new InputStreamReader(zs, CHARSET))
    ) {
      zs.getNextEntry();
      return br.lines()
          .collect(joining("\n"))
          .getBytes(CHARSET);
    }
  }
}
