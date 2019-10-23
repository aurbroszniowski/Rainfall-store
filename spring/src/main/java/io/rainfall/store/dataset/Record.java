package io.rainfall.store.dataset;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
@Getter(AccessLevel.PUBLIC)
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@SuppressWarnings("UnusedDeclaration")
public class Record<V> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private Date created;
  private Date updated;

  @PrePersist
  protected void onCreate() {
    created = new Date();
    updated = created;
  }

  @PreUpdate
  protected void onUpdate() {
    updated = new Date();
  }

  @Embedded
  @Basic(fetch = FetchType.LAZY)
  @NonNull
  private V value;
}
