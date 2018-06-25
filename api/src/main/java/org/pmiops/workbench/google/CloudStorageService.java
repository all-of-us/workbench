package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.util.List;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readMandrillApiKey();
  public String getImageUrl(String image_name);
  public List<Blob> getBlobList(String bucketName, String directory);
  public void writeFile(String bucketName, String fileName, byte[] bytes);
  public void copyBlob(BlobId from, BlobId to);
}
