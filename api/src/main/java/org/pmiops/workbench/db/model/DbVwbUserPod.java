package org.pmiops.workbench.db.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "vwb_user_pod")
public class DbVwbUserPod {

  private Long vwbUserPodId;
  private DbUser user;
  private String vwbPodId;
  private Boolean isActive;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vwb_user_pod_id")
  public Long getVwbUserPodId() {
    return vwbUserPodId;
  }

  public DbVwbUserPod setVwbUserPodId(Long vwbUserPodId) {
    this.vwbUserPodId = vwbUserPodId;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbVwbUserPod setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @Column(name = "vwb_pod_id")
  public String getVwbPodId() {
    return vwbPodId;
  }

  public DbVwbUserPod setVwbPodId(String vwbPodId) {
    this.vwbPodId = vwbPodId;
    return this;
  }

  @Column(name = "is_active")
  public Boolean isActive() {
    return isActive;
  }

  public DbVwbUserPod setActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbVwbUserPod that = (DbVwbUserPod) o;
    return Objects.equals(vwbUserPodId, that.vwbUserPodId)
        && Objects.equals(vwbPodId, that.vwbPodId)
        && Objects.equals(isActive, that.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vwbUserPodId, vwbPodId, isActive);
  }
}
