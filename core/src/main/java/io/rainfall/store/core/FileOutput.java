package io.rainfall.store.core;


import io.rainfall.store.data.Payload;

import java.util.Objects;


public class FileOutput {

  private final Payload payload;

  FileOutput(Builder builder) {
    payload = builder.payload;
  }

  public Payload getPayload() {
    return payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FileOutput that = (FileOutput)o;
    return Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payload);
  }

  @Override
  public String toString() {
    return "FileOutput{" +
           "payload=" + payload +
           '}';
  }

  public static abstract class Builder<O extends FileOutput, B extends Builder<O, B>>
      implements io.rainfall.store.core.Builder<O> {

    private Payload payload;

    public B data(String data) {
      return payload(Payload.raw(data));
    }

    @SuppressWarnings("unchecked")
    public B payload(Payload payload) {
      this.payload = payload;
      return (B)this;
    }
  }
}
