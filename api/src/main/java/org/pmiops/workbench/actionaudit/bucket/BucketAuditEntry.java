package org.pmiops.workbench.actionaudit.bucket;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class BucketAuditEntry implements Serializable {

  private String petAccount;
  private long fileLengths;
  private String googleProjectId;
  private String bucketName;
  private OffsetDateTime minTime;
  private OffsetDateTime maxTime;

  public String getPetAccount() {
    return petAccount;
  }

  public void setPetAccount(String petAccount) {
    this.petAccount = petAccount;
  }

  public long getFileLengths() {
    return fileLengths;
  }

  public void setFileLengths(long fileLengths) {
    this.fileLengths = fileLengths;
  }

  public OffsetDateTime getMinTime() {
    return minTime;
  }

  public void setMinTime(OffsetDateTime minTime) {
    this.minTime = minTime;
  }

  public OffsetDateTime getMaxTime() {
    return maxTime;
  }

  public void setMaxTime(OffsetDateTime maxTime) {
    this.maxTime = maxTime;
  }

  public String getGoogleProjectId() {
    return googleProjectId;
  }

  public void setGoogleProjectId(String googleProjectId) {
    this.googleProjectId = googleProjectId;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public long getTimeWindowDurationInSeconds() {
    if (minTime == null || maxTime == null) {
      return 0l;
    }
    return ChronoUnit.SECONDS.between(this.minTime, this.maxTime);
  }
}
