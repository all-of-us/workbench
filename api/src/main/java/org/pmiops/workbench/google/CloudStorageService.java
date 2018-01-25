package org.pmiops.workbench.google;

import org.pmiops.workbench.model.FileDetail;
import com.google.cloud.storage.Blob;
import java.util.List;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readBlockscoreApiKey();
  public List<Blob> getBlobList(String bucketName, String directory);
  public void writeFile(String bucketName, String fileName, byte[] bytes);
}
