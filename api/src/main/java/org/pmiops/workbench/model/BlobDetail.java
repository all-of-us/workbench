package org.pmiops.workbench.model;

public class BlobDetail {
  private String blobName;
  private String path;

  public BlobDetail(String blobName, String path) {
    this.blobName = blobName;
    this.path = path;
  }

  public String getBlobName() {
    return blobName;
  }

  public void setBlobName(String blobName) {
    this.blobName = blobName;
  }


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}