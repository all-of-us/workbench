package org.pmiops.workbench.actionaudit.bucket;

public class BucketAuditEntry {

  private String petAccount;
  private long fileLengths;

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
}
