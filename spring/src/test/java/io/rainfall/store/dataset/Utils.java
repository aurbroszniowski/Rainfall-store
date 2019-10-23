package io.rainfall.store.dataset;

import io.rainfall.store.data.Payload;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static io.rainfall.store.data.Payload.raw;

public class Utils {

  public static Payload readBytes(String fileName) throws IOException {
    try (InputStream is = Utils.class.getResourceAsStream(fileName)) {
      byte[] bytes = IOUtils.toByteArray(is);
      return raw(bytes);
    }
  }
}
