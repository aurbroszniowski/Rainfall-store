package io.rainfall.store.record;

import java.util.Objects;

public class Rec<K extends Comparable<K>, V> {

  private final K ID;
  private final V value;
  private final long timeStamp;

  public Rec(K ID, V value, long timeStamp) {
    this.ID = ID;
    this.value = value;
    this.timeStamp = timeStamp;
  }

  public K getID() {
    return ID;
  }

  public V getValue() {
    return value;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Rec<?, ?> rec = (Rec<?, ?>)o;
    return timeStamp == rec.timeStamp &&
           Objects.equals(ID, rec.ID) &&
           Objects.equals(value, rec.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, value, timeStamp);
  }

  @Override
  public String toString() {
    return "Rec{" +
           "ID=" + ID +
           ", value=" + value +
           ", timeStamp=" + timeStamp +
           '}';
  }
}
