package org.pmiops.workbench.db.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "vwb_user_pod")
public class DbVwbUserPod {

  private Long vwbUserPodId;
  private DbUser user;
  private String vwbPodId;
  private Boolean isInitialCreditsActive;
  private Timestamp initialCreditsLastUpdateTime;
  private Double cost;

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

  @Column(name = "is_initial_credit_active")
  public Boolean isInitialCreditsActive() {
    return isInitialCreditsActive;
  }

  public DbVwbUserPod setInitialCreditsActive(Boolean isInitialCreditsActive) {
    this.isInitialCreditsActive = isInitialCreditsActive;
    return this;
  }

  @Column(name = "cost")
  public Double getCost() {
    return cost;
  }

  public DbVwbUserPod setCost(Double cost) {
    this.cost = cost;
    setInitialCreditsLastUpdateTime();
    return this;
  }

  @Column(name = "initial_credits_last_update_time")
  public Timestamp getInitialCreditsLastUpdateTime() {
    return initialCreditsLastUpdateTime;
  }

  public DbVwbUserPod setInitialCreditsLastUpdateTime(Timestamp initialCreditsLastUpdateTime) {
    this.initialCreditsLastUpdateTime = new Timestamp(Instant.now().toEpochMilli());
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbVwbUserPod that = (DbVwbUserPod) o;
    return Objects.equals(vwbUserPodId, that.vwbUserPodId)
        && Objects.equals(vwbPodId, that.vwbPodId)
        && Objects.equals(isInitialCreditsActive, that.isInitialCreditsActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vwbUserPodId, vwbPodId, isInitialCreditsActive);
  }

  private DbVwbUserPod setInitialCreditsLastUpdateTime() {
    this.initialCreditsLastUpdateTime = Timestamp.from(Instant.now());
    return this;
  }
}
