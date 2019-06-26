package org.pmiops.workbench.google;

import com.google.cloud.storage.BlobId;

public class GoogleCloudLocators {
  public final BlobId blobId;
  public final String fullPath;

  public GoogleCloudLocators(BlobId blobId, String fullPath) {
    this.blobId = blobId;
    this.fullPath = fullPath;
  }
}
