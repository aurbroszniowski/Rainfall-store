package io.rainfall.store.record;

import java.util.Objects;

public class ChildRec<P extends Comparable<P>, K extends Comparable<K>, V> extends Rec<K, V> {

  private final P parentID;

  ChildRec(P parentID, K ID, V value, long timeStamp) {
    super(ID, value, timeStamp);
    this.parentID = parentID;
  }

  public P getParentID() {
    return parentID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ChildRec<?, ?, ?> childRec = (ChildRec<?, ?, ?>)o;
    return Objects.equals(parentID, childRec.parentID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parentID);
  }

  @Override
  public String toString() {
    return "ChildRec{" +
           "parentID=" + parentID +
           "} " + super.toString();
  }
}
