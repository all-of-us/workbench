package org.pmiops.workbench.google;

import com.google.cloud.storage.BlobId;
import java.util.Objects;

public class GoogleCloudLocators {
  public final BlobId blobId;
  public final String fullPath;

  public GoogleCloudLocators(BlobId blobId, String fullPath) {
    this.blobId = blobId;
    this.fullPath = fullPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(blobId, fullPath);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof GoogleCloudLocators)) {
      return false;
    }
    GoogleCloudLocators that = (GoogleCloudLocators) obj;
    return this.blobId.equals(that.blobId)
        && this.fullPath.equals(that.fullPath);
  }
}
